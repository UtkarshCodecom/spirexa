import { v4 as uuidv4 } from 'uuid';
import * as albumsRepository from './albums.repository';
import * as mediaRepository from '../media/media.repository';
import * as mediaService from '../media/media.service';
import type { MediaItem } from '../media/media.service';
import { NotFoundError, BadRequestError } from '../../utils/errors';
import logger from '../../utils/logger';

export interface Album {
  id: string;
  ownerId: string;
  title: string;
  description?: string;
  coverMediaId?: string;
  mediaCount: number;
  createdAt: Date;
  updatedAt: Date;
  /** Only populated by getAlbum (album detail), not listAlbums. */
  media?: MediaItem[];
}

export interface AlbumMedia {
  mediaId: string;
  addedAt: Date;
}

export async function listAlbums(uid: string): Promise<Album[]> {
  const albums = await albumsRepository.findByOwner(uid);
  return albums.map(mapToAlbum);
}

export async function getAlbum(uid: string, albumId: string): Promise<Album> {
  const album = await albumsRepository.findById(uid, albumId);

  if (!album) {
    throw new NotFoundError('Album');
  }

  if (album.ownerId !== uid) {
    throw new NotFoundError('Album');
  }

  const albumMedia = await albumsRepository.getAlbumMedia(uid, albumId);
  const media = await mediaService.getMediaItemsByIds(
    uid,
    albumMedia.map((m) => m.mediaId)
  );

  return { ...mapToAlbum(album), media };
}

export async function createAlbum(
  uid: string,
  data: { title: string; description?: string; coverMediaId?: string }
): Promise<Album> {
  const albumId = uuidv4();

  const album = await albumsRepository.create({
    id: albumId,
    ownerId: uid,
    title: data.title,
    description: data.description,
    coverMediaId: data.coverMediaId,
  });

  logger.info({ uid, albumId, title: data.title }, 'Album created');
  return mapToAlbum(album);
}

export async function updateAlbum(
  uid: string,
  albumId: string,
  data: { title?: string; description?: string; coverMediaId?: string }
): Promise<Album> {
  const existingAlbum = await albumsRepository.findById(uid, albumId);

  if (!existingAlbum) {
    throw new NotFoundError('Album');
  }

  if (existingAlbum.ownerId !== uid) {
    throw new NotFoundError('Album');
  }

  const updated = await albumsRepository.update(uid, albumId, data);

  if (!updated) {
    throw new NotFoundError('Album');
  }

  return mapToAlbum(updated);
}

export async function deleteAlbum(uid: string, albumId: string): Promise<void> {
  const album = await albumsRepository.findById(uid, albumId);

  if (!album) {
    throw new NotFoundError('Album');
  }

  if (album.ownerId !== uid) {
    throw new NotFoundError('Album');
  }

  await albumsRepository.remove(uid, albumId);
  logger.info({ uid, albumId }, 'Album deleted');
}

export async function addMediaToAlbum(
  uid: string,
  albumId: string,
  mediaIds: string[]
): Promise<void> {
  const album = await albumsRepository.findById(uid, albumId);

  if (!album) {
    throw new NotFoundError('Album');
  }

  if (album.ownerId !== uid) {
    throw new NotFoundError('Album');
  }

  // Verify all media items exist and belong to the user
  for (const mediaId of mediaIds) {
    const media = await mediaRepository.findById(uid, mediaId);
    if (!media) {
      throw new NotFoundError(`Media ${mediaId}`);
    }
    if (media.ownerId !== uid) {
      throw new NotFoundError(`Media ${mediaId}`);
    }
  }

  for (const mediaId of mediaIds) {
    await albumsRepository.addMedia(uid, albumId, mediaId);
  }

  logger.info({ uid, albumId, mediaCount: mediaIds.length }, 'Media added to album');
}

export async function removeMediaFromAlbum(
  uid: string,
  albumId: string,
  mediaId: string
): Promise<void> {
  const album = await albumsRepository.findById(uid, albumId);

  if (!album) {
    throw new NotFoundError('Album');
  }

  if (album.ownerId !== uid) {
    throw new NotFoundError('Album');
  }

  const removed = await albumsRepository.removeMedia(uid, albumId, mediaId);

  if (!removed) {
    throw new NotFoundError('Media in album');
  }

  logger.info({ uid, albumId, mediaId }, 'Media removed from album');
}

export async function getAlbumMediaList(
  uid: string,
  albumId: string
): Promise<AlbumMedia[]> {
  const album = await albumsRepository.findById(uid, albumId);

  if (!album) {
    throw new NotFoundError('Album');
  }

  if (album.ownerId !== uid) {
    throw new NotFoundError('Album');
  }

  const media = await albumsRepository.getAlbumMedia(uid, albumId);
  return media.map((m) => ({
    mediaId: m.mediaId,
    addedAt: m.addedAt,
  }));
}

function mapToAlbum(data: albumsRepository.AlbumData): Album {
  return {
    id: data.id,
    ownerId: data.ownerId,
    title: data.title,
    description: data.description,
    coverMediaId: data.coverMediaId,
    mediaCount: data.mediaCount,
    createdAt: data.createdAt,
    updatedAt: data.updatedAt,
  };
}
