import { z } from 'zod';

export const paginationSchema = z.object({
  limit: z
    .string()
    .optional()
    .default('20')
    .transform(Number)
    .pipe(z.number().int().min(1).max(200)),
  cursor: z.string().optional(),
});

export const mediaListQuerySchema = paginationSchema.extend({
  favorite: z
    .enum(['true', 'false'])
    .optional()
    .transform((v) => (v === undefined ? undefined : v === 'true')),
  deleted: z
    .enum(['true', 'false'])
    .optional()
    .transform((v) => (v === undefined ? undefined : v === 'true')),
});

export const mediaUpdateSchema = z.object({
  fileName: z.string().min(1).max(255).optional(),
  description: z.string().max(1000).optional(),
  metadata: z
    .object({
      dateTaken: z.string().datetime().optional(),
      location: z
        .object({
          latitude: z.number().min(-90).max(90),
          longitude: z.number().min(-180).max(180),
        })
        .optional(),
    })
    .optional(),
});

export const albumCreateSchema = z.object({
  title: z.string().min(1).max(255),
  description: z.string().max(1000).optional(),
  coverMediaId: z.string().optional(),
});

export const albumUpdateSchema = z.object({
  title: z.string().min(1).max(255).optional(),
  description: z.string().max(1000).optional(),
  coverMediaId: z.string().optional(),
});

export const albumMediaSchema = z.object({
  mediaIds: z.array(z.string()).min(1),
});

export const searchSchema = z.object({
  q: z.string().optional(),
  type: z.enum(['all', 'photos', 'videos']).optional().default('all'),
  favorite: z
    .string()
    .optional()
    .transform((val) => val === 'true'),
  startDate: z.string().datetime().optional(),
  endDate: z.string().datetime().optional(),
  limit: z
    .string()
    .optional()
    .default('20')
    .transform(Number)
    .pipe(z.number().int().min(1).max(200)),
  cursor: z.string().optional(),
});

export const shareCreateSchema = z.object({
  title: z.string().min(1).max(255),
  mediaIds: z.array(z.string()).min(1).max(500),
});

export const userUpdateSchema = z.object({
  displayName: z.string().min(1).max(100).optional(),
  photoURL: z.string().url().optional(),
});
