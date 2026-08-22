import { configureOutboundProxy } from './config/proxy';

// Must run before anything makes an outbound fetch (Firebase Admin's token
// verification does so immediately on first authenticated request).
configureOutboundProxy();

import app from './app';
import { config } from './config';
import { initializeFirebase } from './config/firebase';
import logger from './utils/logger';

// Initialize Firebase
initializeFirebase();

const server = app.listen(config.port, () => {
  logger.info(
    {
      port: config.port,
      env: config.nodeEnv,
    },
    'Server started'
  );
});

// Handle unhandled rejections
process.on('unhandledRejection', (reason, promise) => {
  logger.error({ reason, promise }, 'Unhandled Rejection');
});

// Handle uncaught exceptions
process.on('uncaughtException', (error) => {
  logger.fatal({ error }, 'Uncaught Exception');
  process.exit(1);
});

// Graceful shutdown
const gracefulShutdown = (signal: string) => {
  logger.info(`${signal} received. Starting graceful shutdown...`);

  server.close(() => {
    logger.info('HTTP server closed');
    process.exit(0);
  });

  // Force shutdown after 30 seconds
  setTimeout(() => {
    logger.error('Forced shutdown due to timeout');
    process.exit(1);
  }, 30000);
};

process.on('SIGTERM', () => gracefulShutdown('SIGTERM'));
process.on('SIGINT', () => gracefulShutdown('SIGINT'));

export default server;
