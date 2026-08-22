import { Router } from 'express';
import { authenticate } from '../../middleware/authenticate';
import { validate } from '../../middleware/validate';
import { userUpdateSchema } from '../../validators';
import { getProfile, updateProfile, deleteAccount, getQuota } from './users.controller';

const router = Router();

// @ts-ignore - Express handler type mismatch with AuthenticatedRequest
router.get('/me', authenticate, getProfile);
// @ts-ignore - Express handler type mismatch with AuthenticatedRequest
router.get('/me/quota', authenticate, getQuota);
// @ts-ignore - Express handler type mismatch with AuthenticatedRequest
router.patch('/me', authenticate, validate(userUpdateSchema), updateProfile);
// @ts-ignore - Express handler type mismatch with AuthenticatedRequest
router.delete('/me', authenticate, deleteAccount);

export default router;
