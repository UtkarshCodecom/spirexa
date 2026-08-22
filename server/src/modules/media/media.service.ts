import { v4 as uuidv4 } from 'uuid';
import { Readable } from 'stream';
import { config } from '../../config';
import * as storage from '../../services/storage.service';
import * as aiService from '../../services/ai.service';
import * as mediaRepository from './media.repository';
import * as usersRepository from '../users/users.repository';
import { PaginationParams, PaginationResult } from '../../types';
import { NotFoundError, BadRequestError, ConflictError } from '../../utils/errors';
import { signContentToken } from '../../utils/contentToken';
import logger from '../../utils/logger';

export interface MediaItem {
  id: string;
  ownerId: string;
  fileName: string;
  mimeType: string;
  size: number;
  storageUrl: string;
  width?: number;
  height?: number;
  description?: string;
  favorite: boolean;
  deleted: boolean;
  deletedAt?: Date;
  metadata?: {
    dateTaken?: Date;
    location?: {
      latitude: number;
      longitude: number;
    };
    aiCaption?: string;
    aiTags?: string[];
    isDocument?: boolean;
    documentText?: string;
  };
  createdAt: Date;
  updatedAt: Date;
}

export interface UploadMediaInput {
  fileName: string;
  mimeType: string;
  declaredSize?: number;
  width?: number;
  height?: number;
  takenAt?: Date;
  latitude?: number;
  longitude?: number;
  stream: Readable;
}

const ALLOWED_MIME_TYPES = [
  'image/jpeg',
  'image/png',
  'image/gif',
  'image/webp',
  'image/heic',
  'image/heif',
  'video/mp4',
  'video/quicktime',
  'video/x-msvideo',
  'video/webm',
];

export async function listMedia(
  uid: string,
  pagination: PaginationParams,
  filters: mediaRepository.MediaListFilters = {}
): Promise<PaginationResult<MediaItem>> {
  const result = await mediaRepository.findByOwner(
    uid,
    pagination.limit,
    pagination.cursor,
    filters
  );

  return {
    data: result.data.map((m) => mapToMediaItem(uid, m)),
    nextCursor: result.nextCursor,
    hasMore: !!result.nextCursor,
  };
}

/**
 * Batch-fetch media by id, scoped to the owner — used to build album detail
 * views and public share pages. [ttlSeconds] widens the signed content-token
 * lifetime for the latter (a shared link may sit open far longer than the
 * default 15-minute token used everywhere else).
 */
export async function getMediaItemsByIds(
  uid: string,
  ids: string[],
  ttlSeconds?: number
): Promise<MediaItem[]> {
  const results = await Promise.all(ids.map((id) => mediaRepository.findById(uid, id)));
  return results
    .filter((m): m is mediaRepository.MediaData => !!m && m.ownerId === uid && !m.deleted)
    .map((m) => mapToMediaItem(uid, m, ttlSeconds));
}

export async function getMedia(uid: string, mediaId: string): Promise<MediaItem> {
  const media = await mediaRepository.findById(uid, mediaId);

  if (!media || media.ownerId !== uid) {
    throw new NotFoundError('Media');
  }

  return mapToMediaItem(uid, media);
}

/**
 * Streams the uploaded file straight through to B2 and writes the Firestore
 * record in one call — the client never sees a storage credential or URL.
 * Trusts the byte count B2 actually wrote, not whatever size the client claimed.
 */
export async function uploadMedia(uid: string, input: UploadMediaInput): Promise<MediaItem> {
  if (!ALLOWED_MIME_TYPES.includes(input.mimeType)) {
    throw new BadRequestError(`File type ${input.mimeType} is not supported`);
  }

  if (input.declaredSize && input.declaredSize > config.upload.maxFileSizeBytes) {
    throw new BadRequestError(
      `File size exceeds maximum of ${config.upload.maxFileSizeBytes} bytes`
    );
  }

  const usedBytes = await usersRepository.getStorageUsedBytes(uid);
  if (input.declaredSize && usedBytes + input.declaredSize > config.upload.defaultQuotaBytes) {
    throw new ConflictError('Storage quota exceeded');
  }

  const mediaId = uuidv4();
  const storagePath = storage.buildStorageKey(uid, mediaId, input.fileName);

  const result = await storage.uploadStream(storagePath, input.stream, input.mimeType);

  if (result.bytesWritten > config.upload.maxFileSizeBytes) {
    await storage.deleteObject(storagePath).catch(() => undefined);
    throw new BadRequestError(
      `File size exceeds maximum of ${config.upload.maxFileSizeBytes} bytes`
    );
  }

  if (usedBytes + result.bytesWritten > config.upload.defaultQuotaBytes) {
    await storage.deleteObject(storagePath).catch(() => undefined);
    throw new ConflictError('Storage quota exceeded');
  }

  const media = await mediaRepository.create({
    id: mediaId,
    ownerId: uid,
    fileName: input.fileName,
    mimeType: input.mimeType,
    size: result.bytesWritten,
    storagePath,
    width: input.width,
    height: input.height,
    takenAt: input.takenAt,
    latitude: input.latitude,
    longitude: input.longitude,
  });

  await usersRepository.adjustStorageUsed(uid, result.bytesWritten);

  logger.info({ uid, mediaId, fileName: input.fileName, size: result.bytesWritten }, 'Media uploaded');

  // Fire-and-forget: the upload response doesn't wait on this. Images only —
  // captioning a video's first frame isn't worth the complexity here.
  if (config.gemini.apiKey && input.mimeType.startsWith('image/')) {
    analyzeAndStore(uid, mediaId, storagePath, input.mimeType).catch((error) => {
      logger.warn({ error, uid, mediaId }, 'AI photo analysis failed');
    });
  }

  return mapToMediaItem(uid, media);
}

async function analyzeAndStore(
  uid: string,
  mediaId: string,
  storagePath: string,
  mimeType: string
): Promise<void> {
  const { stream } = await storage.getObjectStream(storagePath);
  const buffer = await streamToBuffer(stream);
  const analysis = await aiService.analyzePhoto(buffer, mimeType);
  if (!analysis) return;

  await mediaRepository.updateAiMetadata(uid, mediaId, analysis);
  logger.info({ uid, mediaId, isDocument: analysis.isDocument }, 'AI photo analysis stored');
}

function streamToBuffer(stream: Readable): Promise<Buffer> {
  return new Promise((resolve, reject) => {
    const chunks: Buffer[] = [];
    stream.on('data', (chunk: Buffer) => chunks.push(chunk));
    stream.on('end', () => resolve(Buffer.concat(chunks)));
    stream.on('error', reject);
  });
}

export interface MediaContent {
  stream: Readable;
  contentType: string;
  contentLength?: number;
  contentRange?: string;
  statusCode: 200 | 206;
}

export async function getMediaContent(
  uid: string,
  mediaId: string,
  range?: string
): Promise<MediaContent> {
  const media = await mediaRepository.findById(uid, mediaId);

  if (!media || media.ownerId !== uid) {
    throw new NotFoundError('Media');
  }

  const result = await storage.getObjectStream(media.storagePath, range);

  return {
    stream: result.stream,
    contentType: result.contentType || media.mimeType,
    contentLength: result.contentLength,
    contentRange: result.contentRange,
    statusCode: result.statusCode,
  };
}

export async function updateMedia(
  uid: string,
  mediaId: string,
  data: {
    fileName?: string;
    description?: string;
    metadata?: {
      dateTaken?: string;
      location?: {
        latitude: number;
        longitude: number;
      };
    };
  }
): Promise<MediaItem> {
  const existingMedia = await mediaRepository.findById(uid, mediaId);

  if (!existingMedia || existingMedia.ownerId !== uid) {
    throw new NotFoundError('Media');
  }

  const updateData: mediaRepository.UpdateMediaData = {};

  if (data.fileName !== undefined) {
    updateData.fileName = data.fileName;
  }

  if (data.description !== undefined) {
    updateData.description = data.description;
  }

  if (data.metadata !== undefined) {
    updateData.metadata = {
      ...(data.metadata.dateTaken && {
        dateTaken: new Date(data.metadata.dateTaken),
      }),
      ...(data.metadata.location && {
        location: data.metadata.location,
      }),
    };
  }

  const updated = await mediaRepository.update(uid, mediaId, updateData);

  if (!updated) {
    throw new NotFoundError('Media');
  }

  return mapToMediaItem(uid, updated);
}

export async function trashMedia(uid: string, mediaId: string): Promise<void> {
  const media = await mediaRepository.findById(uid, mediaId);

  if (!media || media.ownerId !== uid) {
    throw new NotFoundError('Media');
  }

  await mediaRepository.softDelete(uid, mediaId);
  logger.info({ uid, mediaId }, 'Media trashed');
}

export async function toggleFavorite(uid: string, mediaId: string): Promise<MediaItem> {
  const media = await mediaRepository.findById(uid, mediaId);

  if (!media || media.ownerId !== uid) {
    throw new NotFoundError('Media');
  }

  const updated = await mediaRepository.toggleFavorite(uid, mediaId);

  if (!updated) {
    throw new NotFoundError('Media');
  }

  return mapToMediaItem(uid, updated);
}

export async function restoreMedia(uid: string, mediaId: string): Promise<MediaItem> {
  const media = await mediaRepository.findById(uid, mediaId);

  if (!media || media.ownerId !== uid) {
    throw new NotFoundError('Media');
  }

  const restored = await mediaRepository.restore(uid, mediaId);

  if (!restored) {
    throw new NotFoundError('Media');
  }

  return mapToMediaItem(uid, restored);
}

/**
 * On-demand analysis for photos that predate the AI pipeline (or were
 * uploaded before GEMINI_API_KEY was set) — the client calls this to
 * backfill a small, capped batch at a time. Idempotent: a photo that
 * already has a caption is returned as-is rather than re-analyzed, so
 * repeated calls can't rack up repeat Gemini cost.
 */
export async function analyzeMedia(uid: string, mediaId: string): Promise<MediaItem> {
  const media = await mediaRepository.findById(uid, mediaId);

  if (!media || media.ownerId !== uid) {
    throw new NotFoundError('Media');
  }

  if (media.metadata?.aiCaption) {
    return mapToMediaItem(uid, media);
  }

  if (!media.mimeType.startsWith('image/')) {
    throw new BadRequestError('Only images can be analyzed');
  }

  await analyzeAndStore(uid, mediaId, media.storagePath, media.mimeType);

  const updated = await mediaRepository.findById(uid, mediaId);
  return mapToMediaItem(uid, updated!);
}

export async function permanentDelete(uid: string, mediaId: string): Promise<void> {
  const media = await mediaRepository.findById(uid, mediaId);

  if (!media || media.ownerId !== uid) {
    throw new NotFoundError('Media');
  }

  await storage.deleteObject(media.storagePath);
  await mediaRepository.remove(uid, mediaId);
  await usersRepository.adjustStorageUsed(uid, -media.size);

  logger.info({ uid, mediaId, storagePath: media.storagePath }, 'Media permanently deleted');
}

function mapToMediaItem(uid: string, data: mediaRepository.MediaData, ttlSeconds?: number): MediaItem {
  return {
    id: data.id,
    ownerId: data.ownerId,
    fileName: data.fileName,
    mimeType: data.mimeType,
    size: data.size,
    storageUrl: `/api/media/${data.id}/content?token=${signContentToken(uid, data.id, ttlSeconds)}`,
    width: data.width,
    height: data.height,
    description: data.description,
    favorite: data.favorite,
    deleted: data.deleted,
    deletedAt: data.deletedAt,
    metadata: data.metadata,
    createdAt: data.createdAt,
    updatedAt: data.updatedAt,
  };
}
