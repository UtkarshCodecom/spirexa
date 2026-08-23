import crypto from 'crypto';
import dotenv from 'dotenv';
import path from 'path';

dotenv.config({ path: path.resolve(__dirname, '../../.env') });

export interface Config {
  nodeEnv: string;
  port: number;
  firebase: {
    projectId: string;
    clientEmail: string;
    privateKey: string;
  };
  storage: {
    zone: string;
    host: string;
    accessKey: string;
  };
  corsOrigin: string;
  publicWebUrl: string;
  rateLimit: {
    windowMs: number;
    max: number;
  };
  upload: {
    maxFileSizeBytes: number;
    defaultQuotaBytes: number;
  };
  contentTokenSecret: string;
  gemini: {
    apiKey: string;
    model: string;
  };
}

export const config: Config = {
  nodeEnv: process.env.NODE_ENV || 'development',
  port: parseInt(process.env.PORT || '3001', 10),
  firebase: {
    projectId: process.env.FIREBASE_PROJECT_ID || 'photos-ace50',
    clientEmail: process.env.FIREBASE_CLIENT_EMAIL || '',
    privateKey: process.env.FIREBASE_PRIVATE_KEY || '',
  },
  storage: {
    zone: process.env.BUNNY_STORAGE_ZONE || '',
    host: process.env.BUNNY_STORAGE_HOST || 'storage.bunnycdn.com',
    accessKey: process.env.BUNNY_ACCESS_KEY || '',
  },
  corsOrigin: process.env.CORS_ORIGIN || 'http://localhost:3000',
  // Base URL of the public web app — used to build shareable /share/{id} links.
  publicWebUrl: process.env.PUBLIC_WEB_URL || process.env.CORS_ORIGIN || 'http://localhost:3000',
  rateLimit: {
    windowMs: parseInt(process.env.RATE_LIMIT_WINDOW_MS || '60000', 10),
    max: parseInt(process.env.RATE_LIMIT_MAX || '100', 10),
  },
  upload: {
    maxFileSizeBytes: parseInt(process.env.MAX_FILE_SIZE_BYTES || `${200 * 1024 * 1024}`, 10),
    defaultQuotaBytes: parseInt(
      process.env.DEFAULT_QUOTA_BYTES || `${15 * 1024 * 1024 * 1024}`,
      10
    ),
  },
  contentTokenSecret: process.env.CONTENT_TOKEN_SECRET || devOnlySecret(),
  gemini: {
    // Blank disables AI analysis entirely — uploads just skip it, no error.
    apiKey: process.env.GEMINI_API_KEY || '',
    model: process.env.GEMINI_MODEL || 'gemini-3.5-flash-lite',
  },
};

// Signs short-lived media content URLs (see utils/contentToken.ts). A missing
// secret is fine for local dev (tokens just won't survive a server restart);
// production MUST set CONTENT_TOKEN_SECRET or every signed URL breaks on deploy.
function devOnlySecret(): string {
  if (process.env.NODE_ENV === 'production') {
    throw new Error('CONTENT_TOKEN_SECRET must be set in production');
  }
  // eslint-disable-next-line no-console
  console.warn('[config] CONTENT_TOKEN_SECRET not set — using a random dev-only secret.');
  return crypto.randomBytes(32).toString('hex');
}

export default config;
