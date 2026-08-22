import { Response, NextFunction } from 'express';
import { AuthenticatedRequest, getUser } from '../../types';
import { auth } from '../../config/firebase';
import { sendSuccess } from '../../utils/response';
import { NotFoundError } from '../../utils/errors';
import logger from '../../utils/logger';

export async function getMe(
  req: AuthenticatedRequest,
  res: Response,
  next: NextFunction
): Promise<void> {
  try {
    const { uid } = getUser(req);

    const userRecord = await auth.getUser(uid);

    sendSuccess(res, {
      uid: userRecord.uid,
      email: userRecord.email,
      displayName: userRecord.displayName,
      photoURL: userRecord.photoURL,
      emailVerified: userRecord.emailVerified,
      createdAt: userRecord.metadata.creationTime,
      lastSignIn: userRecord.metadata.lastSignInTime,
    });
  } catch (error) {
    if ((error as any).code === 'auth/user-not-found') {
      next(new NotFoundError('User'));
    } else {
      logger.error({ error, uid: getUser(req).uid }, 'Failed to get user profile');
      next(error);
    }
  }
}
