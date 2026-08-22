import crypto from 'crypto';
import { config } from '../config';

/**
 * Short-lived, server-signed tokens that let a specific user view one specific
 * media item's bytes without an Authorization header — needed because plain
 * <img src> (web) and similar can't attach custom headers. Unlike a storage
 * credential, this token is scoped to one uid+mediaId pair and expires quickly,
 * and it's verified only by our own server — B2 credentials are never involved.
 */

const DEFAULT_TTL_SECONDS = 15 * 60;

export function signContentToken(uid: string, mediaId: string, ttlSeconds = DEFAULT_TTL_SECONDS): string {
  const exp = Math.floor(Date.now() / 1000) + ttlSeconds;
  const payload = `${uid}.${mediaId}.${exp}`;
  const sig = crypto.createHmac('sha256', config.contentTokenSecret).update(payload).digest('base64url');
  return Buffer.from(`${payload}.${sig}`).toString('base64url');
}

/** Returns the uid embedded in the token if it's valid, unexpired, and matches [mediaId]. */
export function verifyContentToken(token: string, mediaId: string): string | null {
  try {
    const decoded = Buffer.from(token, 'base64url').toString('utf8');
    const parts = decoded.split('.');
    if (parts.length !== 4) return null;
    const [uid, tokenMediaId, expStr, sig] = parts;

    if (tokenMediaId !== mediaId) return null;

    const exp = parseInt(expStr, 10);
    if (!Number.isFinite(exp) || exp < Math.floor(Date.now() / 1000)) return null;

    const payload = `${uid}.${tokenMediaId}.${expStr}`;
    const expectedSig = crypto.createHmac('sha256', config.contentTokenSecret).update(payload).digest('base64url');

    const sigBuf = Buffer.from(sig);
    const expectedBuf = Buffer.from(expectedSig);
    if (sigBuf.length !== expectedBuf.length || !crypto.timingSafeEqual(sigBuf, expectedBuf)) {
      return null;
    }

    return uid;
  } catch {
    return null;
  }
}
