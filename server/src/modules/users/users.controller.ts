import { Response, NextFunction } from 'express';
import { AuthenticatedRequest, getUser } from '../../types';
import { sendSuccess } from '../../utils/response';
import { NotFoundError } from '../../utils/errors';
import { config } from '../../config';
import * as usersRepository from './users.repository';
import { auth } from '../../config/firebase';
import logger from '../../utils/logger';

export async function getProfile(
  req: AuthenticatedRequest,
  res: Response,
  next: NextFunction
): Promise<void> {
  try {
    const { uid } = getUser(req);

    let user = await usersRepository.findByUid(uid);

    if (!user) {
      // Create profile from Firebase Auth data
      const userRecord = await auth.getUser(uid);
      user = await usersRepository.create({
        uid: userRecord.uid,
        email: userRecord.email || null,
        displayName: userRecord.displayName || null,
        photoURL: userRecord.photoURL || null,
        storageUsedBytes: 0,
        createdAt: new Date(),
        updatedAt: new Date(),
      });
    }

    sendSuccess(res, user);
  } catch (error) {
    logger.error({ error, uid: getUser(req).uid }, 'Failed to get user profile');
    next(error);
  }
}

export async function getQuota(
  req: AuthenticatedRequest,
  res: Response,
  next: NextFunction
): Promise<void> {
  try {
    const { uid } = getUser(req);
    const usedBytes = await usersRepository.getStorageUsedBytes(uid);
    sendSuccess(res, { usedBytes, limitBytes: config.upload.defaultQuotaBytes });
  } catch (error) {
    logger.error({ error, uid: getUser(req).uid }, 'Failed to get storage quota');
    next(error);
  }
}

export async function updateProfile(
  req: AuthenticatedRequest,
  res: Response,
  next: NextFunction
): Promise<void> {
  try {
    const { uid } = getUser(req);
    const updateData = req.body;

    const user = await usersRepository.update(uid, updateData);

    if (!user) {
      return next(new NotFoundError('User'));
    }

    // Also update Firebase Auth profile
    if (updateData.displayName || updateData.photoURL) {
      await auth.updateUser(uid, {
        ...(updateData.displayName && { displayName: updateData.displayName }),
        ...(updateData.photoURL && { photoURL: updateData.photoURL }),
      });
    }

    sendSuccess(res, user);
  } catch (error) {
    logger.error({ error, uid: getUser(req).uid }, 'Failed to update user profile');
    next(error);
  }
}

export async function deleteAccount(
  req: AuthenticatedRequest,
  _res: Response,
  next: NextFunction
): Promise<void> {
  try {
    const { uid } = getUser(req);

    // TODO: Implement full account deletion
    // 1. Delete all media objects from B2 storage
    // 2. Delete all Firestore data (media, albums, etc.)
    // 3. Delete Firebase Auth user

    logger.info({ uid }, 'Account deletion requested (not yet implemented)');

    next(new NotFoundError('Account deletion not yet implemented'));
  } catch (error) {
    logger.error({ error, uid: getUser(req).uid }, 'Failed to delete account');
    next(error);
  }
}
