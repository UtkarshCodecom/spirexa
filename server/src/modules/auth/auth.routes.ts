import { Router } from 'express';
import { authenticate } from '../../middleware/authenticate';
import { getMe } from './auth.controller';

const router = Router();

// @ts-ignore - Express handler type mismatch with AuthenticatedRequest
router.get('/me', authenticate, getMe);

export default router;
