import { Router } from 'express';
import { authenticate, authenticateContent } from '../../middleware/authenticate';
import { validate } from '../../middleware/validate';
import { uploadLimiter } from '../../middleware/rateLimiter';
import { mediaListQuerySchema, mediaUpdateSchema } from '../../validators';
import {
  listMedia,
  getMedia,
  uploadMedia,
  getMediaContent,
  updateMedia,
  toggleFavorite,
  analyzeMedia,
  trashMedia,
  restoreMedia,
  permanentDelete,
} from './media.controller';

const router = Router();

// Streams bytes straight through to storage — deliberately mounted before any
// JSON body parser and uses its own auth (Bearer OR a short-lived signed
// ?token=, since <img>-style consumers of /:id/content can't send headers).
// @ts-ignore - Express handler type mismatch with AuthenticatedRequest
router.post('/upload', authenticate, uploadLimiter, uploadMedia);
// @ts-ignore - Express handler type mismatch with AuthenticatedRequest
router.get('/:id/content', authenticateContent, getMediaContent);

// @ts-ignore - Express middleware type mismatch with AuthenticatedRequest
router.use(authenticate);

// @ts-ignore - Express handler type mismatch with AuthenticatedRequest
router.get('/', validate(mediaListQuerySchema, 'query'), listMedia);
// @ts-ignore - Express handler type mismatch with AuthenticatedRequest
router.get('/:id', getMedia);
// @ts-ignore - Express handler type mismatch with AuthenticatedRequest
router.patch('/:id', validate(mediaUpdateSchema), updateMedia);
// @ts-ignore - Express handler type mismatch with AuthenticatedRequest
router.post('/:id/favorite', toggleFavorite);
// @ts-ignore - Express handler type mismatch with AuthenticatedRequest
router.delete('/:id/favorite', toggleFavorite);
// @ts-ignore - Express handler type mismatch with AuthenticatedRequest
router.post('/:id/analyze', analyzeMedia);
// @ts-ignore - Express handler type mismatch with AuthenticatedRequest
router.post('/:id/trash', trashMedia);
// @ts-ignore - Express handler type mismatch with AuthenticatedRequest
router.post('/:id/restore', restoreMedia);
// @ts-ignore - Express handler type mismatch with AuthenticatedRequest
router.delete('/:id/permanent', permanentDelete);

export default router;
