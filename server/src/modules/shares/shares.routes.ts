import { Router } from 'express';
import { authenticate } from '../../middleware/authenticate';
import { validate } from '../../middleware/validate';
import { shareCreateSchema } from '../../validators';
import { createShare, listMyShares, getPublicShare, deleteShare } from './shares.controller';

const router = Router();

// Public — must stay mounted before router.use(authenticate) below. Only a
// GET by id (the share's own random, unguessable id) is ever unauthenticated.
// @ts-ignore - Express handler type mismatch with AuthenticatedRequest
router.get('/:id', getPublicShare);

// @ts-ignore - Express middleware type mismatch with AuthenticatedRequest
router.use(authenticate);

// @ts-ignore - Express handler type mismatch with AuthenticatedRequest
router.get('/', listMyShares);
// @ts-ignore - Express handler type mismatch with AuthenticatedRequest
router.post('/', validate(shareCreateSchema), createShare);
// @ts-ignore - Express handler type mismatch with AuthenticatedRequest
router.delete('/:id', deleteShare);

export default router;
