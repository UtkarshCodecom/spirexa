import { Response } from 'express';
import { AppError } from '../types';
import { ApiResponse } from '../types';

export function sendSuccess<T>(res: Response, data: T, statusCode: number = 200): void {
  const response: ApiResponse<T> = {
    success: true,
    data,
  };
  res.status(statusCode).json(response);
}

export function sendError(res: Response, error: AppError | Error): void {
  const statusCode = error instanceof AppError ? error.statusCode : 500;
  const code = error instanceof AppError ? error.code : 'INTERNAL_ERROR';
  const message = error.message || 'An unexpected error occurred';

  const response: ApiResponse = {
    success: false,
    error: {
      code,
      message,
    },
  };

  res.status(statusCode).json(response);
}
