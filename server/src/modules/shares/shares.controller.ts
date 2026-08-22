import { Response, NextFunction } from 'express';
import { AuthenticatedRequest, getUser } from '../../types';
import { sendSuccess } from '../../utils/response';
import * as sharesService from './shares.service';
import logger from '../../utils/logger';

export async function createShare(
  req: AuthenticatedRequest,
  res: Response,
  next: NextFunction
): Promise<void> {
  try {
    const { uid } = getUser(req);
    const { title, mediaIds } = req.body;

    const share = await sharesService.createShare(uid, title, mediaIds);
    sendSuccess(res, share, 201);
  } catch (error) {
    logger.error({ error, uid: getUser(req).uid }, 'Failed to create share');
    next(error);
  }
}

export async function listMyShares(
  req: AuthenticatedRequest,
  res: Response,
  next: NextFunction
): Promise<void> {
  try {
    const { uid } = getUser(req);
    const shares = await sharesService.listMyShares(uid);
    sendSuccess(res, shares);
  } catch (error) {
    logger.error({ error, uid: getUser(req).uid }, 'Failed to list shares');
    next(error);
  }
}

/** Public — no authentication. Anyone holding the link can call this. */
export async function getPublicShare(
  req: AuthenticatedRequest,
  res: Response,
  next: NextFunction
): Promise<void> {
  try {
    const { id } = req.params;
    const share = await sharesService.getPublicShare(id);
    sendSuccess(res, share);
  } catch (error) {
    next(error);
  }
}

export async function deleteShare(
  req: AuthenticatedRequest,
  res: Response,
  next: NextFunction
): Promise<void> {
  try {
    const { uid } = getUser(req);
    const { id } = req.params;

    await sharesService.deleteShare(uid, id);
    sendSuccess(res, { message: 'Share revoked' });
  } catch (error) {
    logger.error({ error, uid: getUser(req).uid, shareId: req.params.id }, 'Failed to delete share');
    next(error);
  }
}
