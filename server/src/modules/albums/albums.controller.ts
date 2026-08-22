import { Response, NextFunction } from 'express';
import { AuthenticatedRequest, getUser } from '../../types';
import { sendSuccess } from '../../utils/response';
import * as albumsService from './albums.service';
import logger from '../../utils/logger';

export async function listAlbums(
  req: AuthenticatedRequest,
  res: Response,
  next: NextFunction
): Promise<void> {
  try {
    const { uid } = getUser(req);
    const albums = await albumsService.listAlbums(uid);
    sendSuccess(res, albums);
  } catch (error) {
    logger.error({ error, uid: getUser(req).uid }, 'Failed to list albums');
    next(error);
  }
}

export async function createAlbum(
  req: AuthenticatedRequest,
  res: Response,
  next: NextFunction
): Promise<void> {
  try {
    const { uid } = getUser(req);
    const { title, description, coverMediaId } = req.body;

    const album = await albumsService.createAlbum(uid, {
      title,
      description,
      coverMediaId,
    });

    sendSuccess(res, album, 201);
  } catch (error) {
    logger.error({ error, uid: getUser(req).uid }, 'Failed to create album');
    next(error);
  }
}

export async function getAlbum(
  req: AuthenticatedRequest,
  res: Response,
  next: NextFunction
): Promise<void> {
  try {
    const { uid } = getUser(req);
    const { id } = req.params;

    const album = await albumsService.getAlbum(uid, id);
    sendSuccess(res, album);
  } catch (error) {
    logger.error({ error, uid: getUser(req).uid, albumId: req.params.id }, 'Failed to get album');
    next(error);
  }
}

export async function updateAlbum(
  req: AuthenticatedRequest,
  res: Response,
  next: NextFunction
): Promise<void> {
  try {
    const { uid } = getUser(req);
    const { id } = req.params;

    const album = await albumsService.updateAlbum(uid, id, req.body);
    sendSuccess(res, album);
  } catch (error) {
    logger.error({ error, uid: getUser(req).uid, albumId: req.params.id }, 'Failed to update album');
    next(error);
  }
}

export async function deleteAlbum(
  req: AuthenticatedRequest,
  res: Response,
  next: NextFunction
): Promise<void> {
  try {
    const { uid } = getUser(req);
    const { id } = req.params;

    await albumsService.deleteAlbum(uid, id);
    sendSuccess(res, { message: 'Album deleted' });
  } catch (error) {
    logger.error({ error, uid: getUser(req).uid, albumId: req.params.id }, 'Failed to delete album');
    next(error);
  }
}

export async function addMediaToAlbum(
  req: AuthenticatedRequest,
  res: Response,
  next: NextFunction
): Promise<void> {
  try {
    const { uid } = getUser(req);
    const { id } = req.params;
    const { mediaIds } = req.body;

    await albumsService.addMediaToAlbum(uid, id, mediaIds);
    sendSuccess(res, { message: 'Media added to album' });
  } catch (error) {
    logger.error(
      { error, uid: getUser(req).uid, albumId: req.params.id },
      'Failed to add media to album'
    );
    next(error);
  }
}

export async function removeMediaFromAlbum(
  req: AuthenticatedRequest,
  res: Response,
  next: NextFunction
): Promise<void> {
  try {
    const { uid } = getUser(req);
    const { id, mediaId } = req.params;

    await albumsService.removeMediaFromAlbum(uid, id, mediaId);
    sendSuccess(res, { message: 'Media removed from album' });
  } catch (error) {
    logger.error(
      { error, uid: getUser(req).uid, albumId: req.params.id },
      'Failed to remove media from album'
    );
    next(error);
  }
}

export async function getAlbumMediaList(
  req: AuthenticatedRequest,
  res: Response,
  next: NextFunction
): Promise<void> {
  try {
    const { uid } = getUser(req);
    const { id } = req.params;

    const media = await albumsService.getAlbumMediaList(uid, id);
    sendSuccess(res, media);
  } catch (error) {
    logger.error(
      { error, uid: getUser(req).uid, albumId: req.params.id },
      'Failed to get album media'
    );
    next(error);
  }
}
