import { PassThrough, Readable } from 'stream';
import { config } from '../config';
import { StorageError } from '../utils/errors';
import logger from '../utils/logger';

/**
 * Bunny.net Edge Storage, accessed only from the server.
 *
 * The Bunny access key never leaves this process — clients (Android, web)
 * stream file bytes to the server over the authenticated API, and the server
 * streams them on to Bunny and back. No client ever sees a storage
 * credential or a direct storage URL.
 *
 * Switched from Backblaze B2 mid-hackathon: B2's free-tier download cap was
 * hit during testing with no payment method on file to raise it. Bunny's
 * storage API is a plain PUT/GET/DELETE over HTTPS with an AccessKey header
 * (no AWS SDK, no multipart-upload machinery needed) — small enough to swap
 * in behind the same exported function signatures so nothing else in the
 * server had to change.
 */

function assertConfigured(): void {
  if (!config.storage.zone || !config.storage.accessKey) {
    throw new StorageError('Bunny.net storage is not configured on the server');
  }
}

function objectUrl(key: string): string {
  return `https://${config.storage.host}/${config.storage.zone}/${key}`;
}

const UNSAFE_FILENAME_CHARS = ['/', '\\', ' ', '\t', '\n', '\r'];

/**
 * Turns an arbitrary user-supplied filename into a safe path segment.
 * Deliberately avoids a regex character class here — that approach silently
 * broke under this project's dev-mode transpiler (tsx/esbuild mis-parsed an
 * unescaped "/" inside "[...]"), so plain string replacement is used instead.
 */
export function sanitizeFileName(name: string): string {
  let cleaned = name;
  for (const char of UNSAFE_FILENAME_CHARS) {
    cleaned = cleaned.split(char).join('_');
  }
  cleaned = cleaned.trim();
  const safe = cleaned.length > 0 ? cleaned : 'file';
  return safe.length > 180 ? safe.slice(-180) : safe;
}

export function buildStorageKey(uid: string, mediaId: string, fileName: string): string {
  return `${uid}/${mediaId}_${sanitizeFileName(fileName)}`;
}

export interface UploadResult {
  key: string;
  bytesWritten: number;
  etag?: string;
}

/**
 * Streams [body] straight through to Bunny, without buffering the whole file
 * in memory or writing it to local disk. Returns the exact byte count
 * actually written, which is what we trust for quota accounting — never the
 * client-declared size.
 */
export async function uploadStream(
  key: string,
  body: Readable,
  contentType: string
): Promise<UploadResult> {
  assertConfigured();

  let bytesWritten = 0;
  const counter = new PassThrough();
  body.on('data', (chunk: Buffer) => {
    bytesWritten += chunk.length;
  });
  body.pipe(counter);

  try {
    const response = await fetch(objectUrl(key), {
      method: 'PUT',
      headers: {
        AccessKey: config.storage.accessKey,
        'Content-Type': contentType || 'application/octet-stream',
      },
      body: counter,
      duplex: 'half',
    });

    if (!response.ok) {
      throw new Error(`Bunny upload failed with status ${response.status}`);
    }

    return { key, bytesWritten, etag: response.headers.get('etag') ?? undefined };
  } catch (error) {
    logger.error({ error, key }, 'Bunny upload failed');
    throw new StorageError('Failed to upload file to storage');
  }
}

export interface DownloadResult {
  stream: Readable;
  contentType?: string;
  contentLength?: number;
  contentRange?: string;
  acceptRanges?: string;
  statusCode: 200 | 206;
}

/** Streams an object back from Bunny, honoring an optional HTTP Range header. */
export async function getObjectStream(key: string, range?: string): Promise<DownloadResult> {
  assertConfigured();

  try {
    const response = await fetch(objectUrl(key), {
      method: 'GET',
      headers: {
        AccessKey: config.storage.accessKey,
        ...(range ? { Range: range } : {}),
      },
    });

    if (!response.ok) {
      throw new Error(`Bunny download failed with status ${response.status}`);
    }
    if (!response.body) {
      throw new Error('Bunny download returned no body');
    }

    const contentLength = response.headers.get('content-length');
    return {
      stream: Readable.fromWeb(response.body as import('stream/web').ReadableStream),
      contentType: response.headers.get('content-type') ?? undefined,
      contentLength: contentLength ? parseInt(contentLength, 10) : undefined,
      contentRange: response.headers.get('content-range') ?? undefined,
      acceptRanges: response.headers.get('accept-ranges') ?? undefined,
      statusCode: response.status === 206 ? 206 : 200,
    };
  } catch (error) {
    logger.error({ error, key }, 'Bunny download failed');
    throw new StorageError('Failed to read file from storage');
  }
}

export async function deleteObject(key: string): Promise<void> {
  assertConfigured();

  try {
    const response = await fetch(objectUrl(key), {
      method: 'DELETE',
      headers: { AccessKey: config.storage.accessKey },
    });
    // A 404 here just means it's already gone — fine for our callers, which
    // only ever delete to make sure something isn't there anymore.
    if (!response.ok && response.status !== 404) {
      throw new Error(`Bunny delete failed with status ${response.status}`);
    }
  } catch (error) {
    logger.error({ error, key }, 'Bunny delete failed');
    throw new StorageError('Failed to delete file from storage');
  }
}

export async function headObject(key: string): Promise<{ size: number; contentType?: string } | null> {
  assertConfigured();

  try {
    const response = await fetch(objectUrl(key), {
      method: 'HEAD',
      headers: { AccessKey: config.storage.accessKey },
    });
    if (response.status === 404) return null;
    if (!response.ok) {
      throw new Error(`Bunny head failed with status ${response.status}`);
    }
    const contentLength = response.headers.get('content-length');
    return {
      size: contentLength ? parseInt(contentLength, 10) : 0,
      contentType: response.headers.get('content-type') ?? undefined,
    };
  } catch (error) {
    logger.error({ error, key }, 'Bunny head failed');
    throw new StorageError('Failed to read file metadata from storage');
  }
}
