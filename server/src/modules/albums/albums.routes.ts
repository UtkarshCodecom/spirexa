import { Router } from 'express';
import { authenticate } from '../../middleware/authenticate';
import { validate } from '../../middleware/validate';
import {
  albumCreateSchema,
  albumUpdateSchema,
  albumMediaSchema,
} from '../../validators';
import {
  listAlbums,
  createAlbum,
  getAlbum,
  updateAlbum,
  deleteAlbum,
  addMediaToAlbum,
  removeMediaFromAlbum,
  getAlbumMediaList,
} from './albums.controller';

const router = Router();

// All album routes require authentication
// @ts-ignore - Express middleware type mismatch with AuthenticatedRequest
router.use(authenticate);

// @ts-ignore - Express handler type mismatch with AuthenticatedRequest
router.get('/', listAlbums);
// @ts-ignore - Express handler type mismatch with AuthenticatedRequest
router.post('/', validate(albumCreateSchema), createAlbum);
// @ts-ignore - Express handler type mismatch with AuthenticatedRequest
router.get('/:id', getAlbum);
// @ts-ignore - Express handler type mismatch with AuthenticatedRequest
router.patch('/:id', validate(albumUpdateSchema), updateAlbum);
// @ts-ignore - Express handler type mismatch with AuthenticatedRequest
router.delete('/:id', deleteAlbum);
// @ts-ignore - Express handler type mismatch with AuthenticatedRequest
router.post('/:id/media', validate(albumMediaSchema), addMediaToAlbum);
// @ts-ignore - Express handler type mismatch with AuthenticatedRequest
router.delete('/:id/media/:mediaId', removeMediaFromAlbum);
// @ts-ignore - Express handler type mismatch with AuthenticatedRequest
router.get('/:id/media', getAlbumMediaList);

export default router;
