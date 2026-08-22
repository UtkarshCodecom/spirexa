import { Request as ExpressRequest } from 'express';

export interface AuthenticatedUser {
  uid: string;
  email: string | null;
  displayName: string | null;
}

export interface AuthenticatedRequest extends ExpressRequest {
  user: AuthenticatedUser;
}

export function getUser(req: ExpressRequest): AuthenticatedUser {
  return (req as AuthenticatedRequest).user;
}

export class AppError extends Error {
  public readonly statusCode: number;
  public readonly code: string;
  public readonly isOperational: boolean;

  constructor(
    message: string,
    statusCode: number = 500,
    code: string = 'INTERNAL_ERROR',
    isOperational: boolean = true
  ) {
    super(message);
    this.statusCode = statusCode;
    this.code = code;
    this.isOperational = isOperational;
    Object.setPrototypeOf(this, AppError.prototype);
  }
}

export interface PaginationParams {
  limit: number;
  cursor?: string;
}

export interface PaginationResult<T> {
  data: T[];
  nextCursor?: string;
  hasMore: boolean;
}

export interface ApiResponse<T = unknown> {
  success: boolean;
  data?: T;
  error?: {
    code: string;
    message: string;
  };
}
