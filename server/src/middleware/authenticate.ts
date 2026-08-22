import { Response, NextFunction } from 'express';
import { auth } from '../config/firebase';
import { AuthenticatedRequest } from '../types';
import { AuthenticationError } from '../utils/errors';
import { verifyContentToken } from '../utils/contentToken';
import logger from '../utils/logger';

export async function authenticate(
  req: AuthenticatedRequest,
  _res: Response,
  next: NextFunction
): Promise<void> {
  try {
    const authHeader = req.headers.authorization;

    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      throw new AuthenticationError('Missing or invalid authorization header');
    }

    const token = authHeader.split('Bearer ')[1];

    if (!token) {
      throw new AuthenticationError('Missing authentication token');
    }

    const decodedToken = await auth.verifyIdToken(token);

    req.user = {
      uid: decodedToken.uid,
      email: decodedToken.email || null,
      displayName: decodedToken.name || null,
    };

    next();
  } catch (error) {
    if (error instanceof AuthenticationError) {
      next(error);
    } else {
      logger.error({ error }, 'Token verification failed');
      next(new AuthenticationError('Invalid or expired token'));
    }
  }
}

/**
 * Auth for GET /api/media/:id/content only: accepts either a normal Bearer
 * Firebase ID token, or a short-lived `?token=` signed for this exact media
 * item (see utils/contentToken.ts) — needed because <img src> and similar
 * can't attach an Authorization header.
 */
export async function authenticateContent(
  req: AuthenticatedRequest,
  res: Response,
  next: NextFunction
): Promise<void> {
  const authHeader = req.headers.authorization;
  if (authHeader && authHeader.startsWith('Bearer ')) {
    return authenticate(req, res, next);
  }

  const token = req.query.token as string | undefined;
  const mediaId = req.params.id;

  if (!token || !mediaId) {
    next(new AuthenticationError('Missing authentication token'));
    return;
  }

  const uid = verifyContentToken(token, mediaId);
  if (!uid) {
    next(new AuthenticationError('Invalid or expired content token'));
    return;
  }

  req.user = { uid, email: null, displayName: null };
  next();
}
