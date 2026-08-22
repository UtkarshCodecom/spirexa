import express from 'express';
import helmet from 'helmet';
import cors from 'cors';
import pinoHttp from 'pino-http';
import { config } from './config';
import { generalLimiter } from './middleware/rateLimiter';
import { errorHandler } from './middleware/errorHandler';
import logger from './utils/logger';

import healthRoutes from './modules/health/health.routes';
import authRoutes from './modules/auth/auth.routes';
import usersRoutes from './modules/users/users.routes';
import mediaRoutes from './modules/media/media.routes';
import albumsRoutes from './modules/albums/albums.routes';
import searchRoutes from './modules/search/search.routes';
import sharesRoutes from './modules/shares/shares.routes';

const app = express();

// Security headers. crossOriginResourcePolicy is relaxed from helmet's
// default ('same-origin') to 'cross-origin': this API intentionally serves
// media (GET /api/media/:id/content) to the separate web app origin via
// plain <img>/<video> tags, which load in no-cors mode. 'same-origin' CORP
// silently blocks exactly that — the browser reports it as a generic image
// load failure with no console error, while fetch() to the identical URL
// (cors mode, not no-cors) works fine, which is what made this confusing to
// track down. CORS above already scopes which origins can even reach this
// API; CORP here only controls whether a permitted cross-origin embed is
// allowed to render.
app.use(helmet({ crossOriginResourcePolicy: { policy: 'cross-origin' } }));

// CORS
app.use(
  cors({
    origin: config.corsOrigin,
    credentials: true,
  })
);

// Body parsing
app.use(express.json({ limit: '10mb' }));

// HTTP logging
app.use(
  pinoHttp({
    logger,
    autoLogging: config.nodeEnv !== 'test',
  })
);

// Rate limiting
app.use(generalLimiter);

// Health check (no auth required)
app.use('/api/health', healthRoutes);

// API routes
app.use('/api/auth', authRoutes);
app.use('/api/users', usersRoutes);
app.use('/api/media', mediaRoutes);
app.use('/api/albums', albumsRoutes);
app.use('/api/search', searchRoutes);
app.use('/api/shares', sharesRoutes);

// 404 handler
app.use((_req, res) => {
  res.status(404).json({
    success: false,
    error: {
      code: 'NOT_FOUND',
      message: 'Route not found',
    },
  });
});

// Error handler (must be last)
app.use(errorHandler);

export default app;
