import { Router } from 'express';
import { authenticate } from '../../middleware/authenticate';
import { searchLimiter } from '../../middleware/rateLimiter';
import { searchMedia } from './search.controller';

const router = Router();

// @ts-ignore - Express handler type mismatch with AuthenticatedRequest
router.get('/', authenticate, searchLimiter, searchMedia);

export default router;
