import { Response, NextFunction } from 'express';
import { AuthenticatedRequest, getUser } from '../../types';
import { sendSuccess } from '../../utils/response';
import * as searchService from './search.service';
import logger from '../../utils/logger';

export async function searchMedia(
  req: AuthenticatedRequest,
  res: Response,
  next: NextFunction
): Promise<void> {
  try {
    const { uid } = getUser(req);
    const filters: searchService.SearchFilters = {
      q: req.query.q as string | undefined,
      type: (req.query.type as 'all' | 'photos' | 'videos') || 'all',
      favorite: req.query.favorite === 'true' ? true : undefined,
      startDate: req.query.startDate as string | undefined,
      endDate: req.query.endDate as string | undefined,
      limit: parseInt(req.query.limit as string) || 20,
      cursor: req.query.cursor as string | undefined,
    };

    const result = await searchService.search(uid, filters);
    sendSuccess(res, result);
  } catch (error) {
    logger.error({ error, uid: getUser(req).uid }, 'Search failed');
    next(error);
  }
}
