import { Request, Response, NextFunction } from 'express';
import { AppError } from '../types';
import { config } from '../config';
import logger from '../utils/logger';

export function errorHandler(
  err: Error,
  _req: Request,
  res: Response,
  _next: NextFunction
): void {
  if (err instanceof AppError) {
    logger.warn(
      {
        error: err.message,
        code: err.code,
        statusCode: err.statusCode,
      },
      'AppError caught'
    );

    res.status(err.statusCode).json({
      success: false,
      error: {
        code: err.code,
        message: err.message,
      },
    });
    return;
  }

  logger.error({ error: err.message, stack: err.stack }, 'Unhandled error');

  const statusCode = 500;
  const message =
    config.nodeEnv === 'production'
      ? 'An unexpected error occurred'
      : err.message;

  res.status(statusCode).json({
    success: false,
    error: {
      code: 'INTERNAL_ERROR',
      message,
    },
  });
}
