import { Request, Response, NextFunction } from 'express';
import { ZodSchema, ZodError } from 'zod';
import { ValidationError } from '../utils/errors';

export function validate(
  schema: ZodSchema,
  property: 'body' | 'query' | 'params' = 'body'
) {
  return (req: Request, _res: Response, next: NextFunction): void => {
    try {
      const data = schema.parse(req[property]);
      req[property] = data;
      next();
    } catch (error) {
      if (error instanceof ZodError) {
        const messages = error.errors.map((e) => `${e.path.join('.')}: ${e.message}`);
        next(new ValidationError(messages.join('; ')));
      } else {
        next(error);
      }
    }
  };
}
