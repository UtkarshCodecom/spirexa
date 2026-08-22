import crypto from 'crypto';
import * as sharesRepository from './shares.repository';
import * as mediaService from '../media/media.service';
import { MediaItem } from '../media/media.service';
import { NotFoundError, BadRequestError, AuthorizationError } from '../../utils/errors';
import { config } from '../../config';
import logger from '../../utils/logger';

// A shared link may realistically sit open (or get revisited) well past the
// usual 15-minute content-token window — a full day is generous but bounded.
const SHARE_CONTENT_TTL_SECONDS = 24 * 60 * 60;

export interface Share {
  id: string;
  title: string;
  mediaCount: number;
  createdAt: Date;
  shareUrl: string;
}

export interface SharePublicView {
  id: string;
  title: string;
  createdAt: Date;
  media: MediaItem[];
}

function buildShareUrl(id: string): string {
  return `${config.publicWebUrl.replace(/\/$/, '')}/share/${id}`;
}

export async function createShare(
  uid: string,
  title: string,
  mediaIds: string[]
): Promise<Share> {
  // Only include items the caller actually owns (and that aren't trashed) —
  // this also quietly drops any id typos/duplicates instead of erroring.
  const owned = await mediaService.getMediaItemsByIds(uid, mediaIds);
  if (owned.length === 0) {
    throw new BadRequestError('None of the selected items could be shared');
  }

  const id = crypto.randomBytes(16).toString('base64url');
  const share = await sharesRepository.create({
    id,
    ownerId: uid,
    title,
    mediaIds: owned.map((m) => m.id),
  });

  logger.info({ uid, shareId: id, mediaCount: owned.length }, 'Share created');

  return {
    id: share.id,
    title: share.title,
    mediaCount: share.mediaIds.length,
    createdAt: share.createdAt,
    shareUrl: buildShareUrl(share.id),
  };
}

export async function listMyShares(uid: string): Promise<Share[]> {
  const shares = await sharesRepository.findByOwner(uid);
  return shares.map((s) => ({
    id: s.id,
    title: s.title,
    mediaCount: s.mediaIds.length,
    createdAt: s.createdAt,
    shareUrl: buildShareUrl(s.id),
  }));
}

/** Public, unauthenticated read — anyone with the link can call this. */
export async function getPublicShare(shareId: string): Promise<SharePublicView> {
  const share = await sharesRepository.findById(shareId);
  if (!share) {
    throw new NotFoundError('Share');
  }

  const media = await mediaService.getMediaItemsByIds(
    share.ownerId,
    share.mediaIds,
    SHARE_CONTENT_TTL_SECONDS
  );

  return {
    id: share.id,
    title: share.title,
    createdAt: share.createdAt,
    media,
  };
}

export async function deleteShare(uid: string, shareId: string): Promise<void> {
  const share = await sharesRepository.findById(shareId);
  if (!share) {
    throw new NotFoundError('Share');
  }
  if (share.ownerId !== uid) {
    throw new AuthorizationError('You do not own this share');
  }

  await sharesRepository.remove(shareId);
  logger.info({ uid, shareId }, 'Share revoked');
}
