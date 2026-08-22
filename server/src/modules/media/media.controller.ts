import { Response, NextFunction } from 'express';
import Busboy from 'busboy';
import { AuthenticatedRequest, getUser } from '../../types';
import { sendSuccess } from '../../utils/response';
import { BadRequestError } from '../../utils/errors';
import { config } from '../../config';
import * as mediaService from './media.service';
import logger from '../../utils/logger';

export async function listMedia(
  req: AuthenticatedRequest,
  res: Response,
  next: NextFunction
): Promise<void> {
  try {
    const { uid } = getUser(req);
    const limit = parseInt(req.query.limit as string) || 20;
    const cursor = req.query.cursor as string | undefined;
    const favorite = req.query.favorite as boolean | undefined;
    const deleted = req.query.deleted as boolean | undefined;

    const result = await mediaService.listMedia(uid, { limit, cursor }, { favorite, deleted });
    sendSuccess(res, result);
  } catch (error) {
    logger.error({ error, uid: getUser(req).uid }, 'Failed to list media');
    next(error);
  }
}

export async function getMedia(
  req: AuthenticatedRequest,
  res: Response,
  next: NextFunction
): Promise<void> {
  try {
    const { uid } = getUser(req);
    const { id } = req.params;

    const media = await mediaService.getMedia(uid, id);
    sendSuccess(res, media);
  } catch (error) {
    logger.error({ error, uid: getUser(req).uid, mediaId: req.params.id }, 'Failed to get media');
    next(error);
  }
}

/**
 * Accepts a multipart/form-data upload and streams the file straight through
 * to storage — the client posts fields (fileName, mimeType, size, width,
 * height, takenAt) followed by the "file" part. Fields must arrive before the
 * file part in the request body (our own Android/web upload code guarantees
 * this); everything is applied to the file stream as it starts flowing.
 */
export async function uploadMedia(
  req: AuthenticatedRequest,
  res: Response,
  next: NextFunction
): Promise<void> {
  const { uid } = getUser(req);
  let responded = false;

  let busboy: Busboy.Busboy;
  try {
    busboy = Busboy({
      headers: req.headers,
      limits: { fileSize: config.upload.maxFileSizeBytes, files: 1 },
    });
  } catch (error) {
    next(new BadRequestError('Invalid upload request'));
    return;
  }

  const fields: Record<string, string> = {};
  let sawFile = false;

  busboy.on('field', (name, value) => {
    fields[name] = value;
  });

  busboy.on('file', (_name, fileStream, info) => {
    sawFile = true;
    const fileName = fields.fileName || info.filename || 'file';
    const mimeType = fields.mimeType || info.mimeType;

    // busboy truncates (doesn't error) a file that exceeds the configured
    // limit — without this flag the truncated, smaller file would sail past
    // the post-upload size check and get saved as if it were complete.
    let truncated = false;
    fileStream.on('limit', () => {
      truncated = true;
    });

    mediaService
      .uploadMedia(uid, {
        fileName,
        mimeType,
        declaredSize: fields.size ? parseInt(fields.size, 10) : undefined,
        width: fields.width ? parseInt(fields.width, 10) : undefined,
        height: fields.height ? parseInt(fields.height, 10) : undefined,
        takenAt: fields.takenAt ? new Date(fields.takenAt) : undefined,
        latitude: fields.latitude ? parseFloat(fields.latitude) : undefined,
        longitude: fields.longitude ? parseFloat(fields.longitude) : undefined,
        stream: fileStream,
      })
      .then(async (media) => {
        if (truncated) {
          await mediaService.permanentDelete(uid, media.id).catch(() => undefined);
          responded = true;
          next(new BadRequestError(`File size exceeds maximum of ${config.upload.maxFileSizeBytes} bytes`));
          return;
        }
        responded = true;
        sendSuccess(res, media, 201);
      })
      .catch((error) => {
        responded = true;
        fileStream.resume();
        logger.error({ error, uid }, 'Failed to upload media');
        next(error);
      });
  });

  busboy.on('error', (error) => {
    if (!responded) {
      responded = true;
      next(error);
    }
  });

  busboy.on('finish', () => {
    if (!sawFile && !responded) {
      responded = true;
      next(new BadRequestError('No file provided'));
    }
  });

  req.pipe(busboy);
}

export async function getMediaContent(
  req: AuthenticatedRequest,
  res: Response,
  next: NextFunction
): Promise<void> {
  try {
    const { uid } = getUser(req);
    const { id } = req.params;
    const range = req.headers.range;

    const content = await mediaService.getMediaContent(uid, id, range);

    res.status(content.statusCode);
    res.setHeader('Content-Type', content.contentType);
    res.setHeader('Accept-Ranges', 'bytes');
    res.setHeader('Cache-Control', 'private, max-age=86400');
    if (content.contentLength !== undefined) {
      res.setHeader('Content-Length', content.contentLength.toString());
    }
    if (content.contentRange) {
      res.setHeader('Content-Range', content.contentRange);
    }

    content.stream.on('error', (error) => next(error));
    content.stream.pipe(res);
  } catch (error) {
    logger.error({ error, uid: getUser(req).uid, mediaId: req.params.id }, 'Failed to stream media');
    next(error);
  }
}

export async function updateMedia(
  req: AuthenticatedRequest,
  res: Response,
  next: NextFunction
): Promise<void> {
  try {
    const { uid } = getUser(req);
    const { id } = req.params;

    const media = await mediaService.updateMedia(uid, id, req.body);
    sendSuccess(res, media);
  } catch (error) {
    logger.error({ error, uid: getUser(req).uid, mediaId: req.params.id }, 'Failed to update media');
    next(error);
  }
}

export async function toggleFavorite(
  req: AuthenticatedRequest,
  res: Response,
  next: NextFunction
): Promise<void> {
  try {
    const { uid } = getUser(req);
    const { id } = req.params;

    const media = await mediaService.toggleFavorite(uid, id);
    sendSuccess(res, media);
  } catch (error) {
    logger.error({ error, uid: getUser(req).uid, mediaId: req.params.id }, 'Failed to toggle favorite');
    next(error);
  }
}

export async function analyzeMedia(
  req: AuthenticatedRequest,
  res: Response,
  next: NextFunction
): Promise<void> {
  try {
    const { uid } = getUser(req);
    const { id } = req.params;

    const media = await mediaService.analyzeMedia(uid, id);
    sendSuccess(res, media);
  } catch (error) {
    logger.error({ error, uid: getUser(req).uid, mediaId: req.params.id }, 'Failed to analyze media');
    next(error);
  }
}

export async function trashMedia(
  req: AuthenticatedRequest,
  res: Response,
  next: NextFunction
): Promise<void> {
  try {
    const { uid } = getUser(req);
    const { id } = req.params;

    await mediaService.trashMedia(uid, id);
    sendSuccess(res, { message: 'Media trashed' });
  } catch (error) {
    logger.error({ error, uid: getUser(req).uid, mediaId: req.params.id }, 'Failed to trash media');
    next(error);
  }
}

export async function restoreMedia(
  req: AuthenticatedRequest,
  res: Response,
  next: NextFunction
): Promise<void> {
  try {
    const { uid } = getUser(req);
    const { id } = req.params;

    const media = await mediaService.restoreMedia(uid, id);
    sendSuccess(res, media);
  } catch (error) {
    logger.error({ error, uid: getUser(req).uid, mediaId: req.params.id }, 'Failed to restore media');
    next(error);
  }
}

export async function permanentDelete(
  req: AuthenticatedRequest,
  res: Response,
  next: NextFunction
): Promise<void> {
  try {
    const { uid } = getUser(req);
    const { id } = req.params;

    await mediaService.permanentDelete(uid, id);
    sendSuccess(res, { message: 'Media permanently deleted' });
  } catch (error) {
    logger.error(
      { error, uid: getUser(req).uid, mediaId: req.params.id },
      'Failed to permanently delete media'
    );
    next(error);
  }
}
