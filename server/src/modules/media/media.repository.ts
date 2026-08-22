import { db } from '../../config/firebase';
import { DocumentSnapshot } from 'firebase-admin/firestore';

export interface MediaData {
  id: string;
  ownerId: string;
  fileName: string;
  mimeType: string;
  size: number;
  storagePath: string;
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

export interface CreateMediaData {
  id: string;
  ownerId: string;
  fileName: string;
  mimeType: string;
  size: number;
  storagePath: string;
  width?: number;
  height?: number;
  takenAt?: Date;
  latitude?: number;
  longitude?: number;
}

export interface UpdateMediaData {
  fileName?: string;
  description?: string;
  metadata?: {
    dateTaken?: Date;
    location?: {
      latitude: number;
      longitude: number;
    };
  };
}

function getMediaCollection(uid: string) {
  return db.collection('users').doc(uid).collection('media');
}

export interface MediaListFilters {
  /** Defaults to false (only non-trashed items) unless explicitly true. */
  deleted?: boolean;
  favorite?: boolean;
}

export async function findByOwner(
  uid: string,
  limit: number = 20,
  cursor?: string,
  filters: MediaListFilters = {}
): Promise<{ data: MediaData[]; nextCursor?: string }> {
  // `deleted` is a real Firestore equality filter — combined with the
  // orderBy below, that's the same single-equality-filter shape already
  // proven to work without needing a manual composite index.
  //
  // `favorite` is applied in-memory after the fetch instead, the same
  // approach search.service.ts already uses for its text filter — adding it
  // as a second Firestore equality filter alongside the orderBy would need
  // a composite index that doesn't exist in this project yet.
  let query = getMediaCollection(uid)
    .where('deleted', '==', filters.deleted === true)
    .orderBy('createdAt', 'desc')
    .limit(limit + 1);

  if (cursor) {
    const cursorDoc = await getMediaCollection(uid).doc(cursor).get();
    if (cursorDoc.exists) {
      query = query.startAfter(cursorDoc);
    }
  }

  const snapshot = await query.get();
  let data: MediaData[] = [];

  snapshot.forEach((doc) => {
    data.push(docToMedia(doc));
  });

  let nextCursor: string | undefined;
  if (data.length > limit) {
    const lastItem = data.pop();
    nextCursor = lastItem?.id;
  }

  if (filters.favorite !== undefined) {
    data = data.filter((m) => m.favorite === filters.favorite);
  }

  return { data, nextCursor };
}

export async function findById(
  uid: string,
  mediaId: string
): Promise<MediaData | null> {
  const doc = await getMediaCollection(uid).doc(mediaId).get();
  if (!doc.exists) {
    return null;
  }
  return docToMedia(doc);
}

export async function create(data: CreateMediaData): Promise<MediaData> {
  const now = new Date();
  const location =
    data.latitude !== undefined && data.longitude !== undefined
      ? { latitude: data.latitude, longitude: data.longitude }
      : undefined;
  const metadata =
    data.takenAt || location
      ? {
          ...(data.takenAt && { dateTaken: data.takenAt }),
          ...(location && { location }),
        }
      : undefined;
  const mediaData = {
    ownerId: data.ownerId,
    fileName: data.fileName,
    mimeType: data.mimeType,
    size: data.size,
    storagePath: data.storagePath,
    width: data.width,
    height: data.height,
    favorite: false,
    deleted: false,
    metadata,
    createdAt: now,
    updatedAt: now,
  };

  await getMediaCollection(data.ownerId).doc(data.id).set(mediaData);

  return { id: data.id, ...mediaData };
}

export async function update(
  uid: string,
  mediaId: string,
  data: UpdateMediaData
): Promise<MediaData | null> {
  const docRef = getMediaCollection(uid).doc(mediaId);
  const doc = await docRef.get();

  if (!doc.exists) {
    return null;
  }

  // Dot-notation keys so this merges into the existing `metadata` map
  // instead of replacing it wholesale (Firestore's update() overwrites a
  // nested object field entirely when given as a plain nested value).
  const updateData: Record<string, unknown> = { updatedAt: new Date() };
  if (data.fileName !== undefined) updateData.fileName = data.fileName;
  if (data.description !== undefined) updateData.description = data.description;
  if (data.metadata?.dateTaken !== undefined) updateData['metadata.dateTaken'] = data.metadata.dateTaken;
  if (data.metadata?.location !== undefined) updateData['metadata.location'] = data.metadata.location;

  await docRef.update(updateData);
  const updatedDoc = await docRef.get();
  return docToMedia(updatedDoc);
}

/**
 * Server-only write for the background AI analysis pass — deliberately not
 * routed through the public `update()`/PATCH path (and its request-body
 * validator), since a client should never be able to set its own caption,
 * tags, or document classification. Silently no-ops if the doc is gone by
 * the time analysis finishes (e.g. the photo was deleted mid-analysis).
 */
export async function updateAiMetadata(
  uid: string,
  mediaId: string,
  analysis: { caption: string; tags: string[]; isDocument: boolean; documentText?: string }
): Promise<void> {
  const docRef = getMediaCollection(uid).doc(mediaId);
  const doc = await docRef.get();
  if (!doc.exists) return;

  const updateData: Record<string, unknown> = {
    updatedAt: new Date(),
    'metadata.aiCaption': analysis.caption,
    'metadata.aiTags': analysis.tags,
    'metadata.isDocument': analysis.isDocument,
  };
  if (analysis.documentText) updateData['metadata.documentText'] = analysis.documentText;

  await docRef.update(updateData);
}

export async function remove(uid: string, mediaId: string): Promise<boolean> {
  const docRef = getMediaCollection(uid).doc(mediaId);
  const doc = await docRef.get();

  if (!doc.exists) {
    return false;
  }

  await docRef.delete();
  return true;
}

export async function softDelete(
  uid: string,
  mediaId: string
): Promise<MediaData | null> {
  const docRef = getMediaCollection(uid).doc(mediaId);
  const doc = await docRef.get();

  if (!doc.exists) {
    return null;
  }

  await docRef.update({
    deleted: true,
    deletedAt: new Date(),
    updatedAt: new Date(),
  });

  const updatedDoc = await docRef.get();
  return docToMedia(updatedDoc);
}

export async function restore(
  uid: string,
  mediaId: string
): Promise<MediaData | null> {
  const docRef = getMediaCollection(uid).doc(mediaId);
  const doc = await docRef.get();

  if (!doc.exists) {
    return null;
  }

  await docRef.update({
    deleted: false,
    deletedAt: null,
    updatedAt: new Date(),
  });

  const updatedDoc = await docRef.get();
  return docToMedia(updatedDoc);
}

export async function toggleFavorite(
  uid: string,
  mediaId: string
): Promise<MediaData | null> {
  const docRef = getMediaCollection(uid).doc(mediaId);
  const doc = await docRef.get();

  if (!doc.exists) {
    return null;
  }

  const currentFavorite = doc.data()?.favorite || false;

  await docRef.update({
    favorite: !currentFavorite,
    updatedAt: new Date(),
  });

  const updatedDoc = await docRef.get();
  return docToMedia(updatedDoc);
}

export async function count(uid: string): Promise<number> {
  const snapshot = await getMediaCollection(uid)
    .where('deleted', '==', false)
    .count()
    .get();
  return snapshot.data().count;
}

function docToMedia(doc: DocumentSnapshot): MediaData {
  const data = doc.data()!;
  return {
    id: doc.id,
    ownerId: data.ownerId,
    fileName: data.fileName,
    mimeType: data.mimeType,
    size: data.size,
    storagePath: data.storagePath,
    width: data.width,
    height: data.height,
    description: data.description,
    favorite: data.favorite || false,
    deleted: data.deleted || false,
    deletedAt: data.deletedAt?.toDate?.(),
    metadata: data.metadata
      ? {
          dateTaken: data.metadata.dateTaken?.toDate?.(),
          location: data.metadata.location,
          aiCaption: data.metadata.aiCaption,
          aiTags: data.metadata.aiTags,
          isDocument: data.metadata.isDocument,
          documentText: data.metadata.documentText,
        }
      : undefined,
    createdAt: data.createdAt?.toDate?.() || new Date(data.createdAt),
    updatedAt: data.updatedAt?.toDate?.() || new Date(data.updatedAt),
  };
}
