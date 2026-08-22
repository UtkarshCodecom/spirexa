import rateLimit from 'express-rate-limit';
import { config } from '../config';

export const generalLimiter = rateLimit({
  windowMs: config.rateLimit.windowMs,
  max: config.rateLimit.max,
  standardHeaders: true,
  legacyHeaders: false,
  message: {
    success: false,
    error: {
      code: 'RATE_LIMIT_EXCEEDED',
      message: 'Too many requests, please try again later',
    },
  },
});

export const authLimiter = rateLimit({
  windowMs: config.rateLimit.windowMs,
  max: 20,
  standardHeaders: true,
  legacyHeaders: false,
  message: {
    success: false,
    error: {
      code: 'RATE_LIMIT_EXCEEDED',
      message: 'Too many authentication attempts, please try again later',
    },
  },
});

export const uploadLimiter = rateLimit({
  windowMs: config.rateLimit.windowMs,
  max: 50,
  standardHeaders: true,
  legacyHeaders: false,
  message: {
    success: false,
    error: {
      code: 'RATE_LIMIT_EXCEEDED',
      message: 'Too many upload requests, please try again later',
    },
  },
});

export const searchLimiter = rateLimit({
  windowMs: config.rateLimit.windowMs,
  max: 30,
  standardHeaders: true,
  legacyHeaders: false,
  message: {
    success: false,
    error: {
      code: 'RATE_LIMIT_EXCEEDED',
      message: 'Too many search requests, please try again later',
    },
  },
});
