import {
  S3Client,
  GetObjectCommand,
  DeleteObjectCommand,
  HeadObjectCommand,
} from '@aws-sdk/client-s3';
import { Upload } from '@aws-sdk/lib-storage';
import { NodeHttpHandler } from '@smithy/node-http-handler';
import { HttpsProxyAgent } from 'https-proxy-agent';
import { PassThrough, Readable } from 'stream';
import { config } from '../config';
import { getProxyUrl } from '../config/proxy';
import { StorageError } from '../utils/errors';
import logger from '../utils/logger';

/**
 * Backblaze B2 (S3-compatible) storage, accessed only from the server.
 *
 * The B2 application key never leaves this process — clients (Android, web)
 * stream file bytes to the server over the authenticated API, and the server
 * streams them on to B2 and back. No client ever sees a storage credential
 * or a direct storage URL.
 */

let client: S3Client | null = null;

function getClient(): S3Client {
  if (client) return client;
  if (!config.storage.bucket || !config.storage.keyId || !config.storage.applicationKey) {
    throw new StorageError('Backblaze B2 storage is not configured on the server');
  }

  // The AWS SDK v3's default HTTP handler builds its own dedicated
  // http.Agent/https.Agent internally — it does NOT fall back to Node's
  // global agent, so pointing http.globalAgent/https.globalAgent at the
  // proxy (done in config/proxy.ts, which fixed node-fetch) does nothing
  // for this client. This is the fourth distinct HTTP client found today
  // with its own separate proxy blind spot; confirmed by the actual
  // failure mode — B2 reads hung with a socket-level ETIMEDOUT reading the
  // response body, not a clean connection-refused, which only makes sense
  // if the request went out directly instead of through the proxy. AWS
  // SDK v3's documented fix is passing a proxy-aware requestHandler.
  const proxyUrl = getProxyUrl();
  const requestHandler = proxyUrl
    ? new NodeHttpHandler({
        httpsAgent: new HttpsProxyAgent(proxyUrl),
        httpAgent: new HttpsProxyAgent(proxyUrl),
      })
    : undefined;

  client = new S3Client({
    region: config.storage.region,
    endpoint: config.storage.endpoint,
    credentials: {
      accessKeyId: config.storage.keyId,
      secretAccessKey: config.storage.applicationKey,
    },
    ...(requestHandler ? { requestHandler } : {}),
  });
  return client;
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
 * Streams [body] straight through to B2 (multipart under the hood for large
 * files via @aws-sdk/lib-storage), without buffering the whole file in memory
 * or writing it to local disk. Returns the exact byte count actually written,
 * which is what we trust for quota accounting — never the client-declared size.
 */
export async function uploadStream(
  key: string,
  body: Readable,
  contentType: string
): Promise<UploadResult> {
  let bytesWritten = 0;
  const counter = new PassThrough();
  body.on('data', (chunk: Buffer) => {
    bytesWritten += chunk.length;
  });
  body.pipe(counter);

  try {
    const upload = new Upload({
      client: getClient(),
      params: {
        Bucket: config.storage.bucket,
        Key: key,
        Body: counter,
        ContentType: contentType,
      },
      queueSize: 4,
      partSize: 8 * 1024 * 1024,
    });

    const result = await upload.done();
    return { key, bytesWritten, etag: result.ETag };
  } catch (error) {
    logger.error({ error, key }, 'B2 upload failed');
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

/** Streams an object back from B2, honoring an optional HTTP Range header. */
export async function getObjectStream(key: string, range?: string): Promise<DownloadResult> {
  try {
    const response = await getClient().send(
      new GetObjectCommand({
        Bucket: config.storage.bucket,
        Key: key,
        Range: range,
      })
    );

    return {
      stream: response.Body as Readable,
      contentType: response.ContentType,
      contentLength: response.ContentLength,
      contentRange: response.ContentRange,
      acceptRanges: response.AcceptRanges,
      statusCode: range && response.ContentRange ? 206 : 200,
    };
  } catch (error) {
    logger.error({ error, key }, 'B2 download failed');
    throw new StorageError('Failed to read file from storage');
  }
}

export async function deleteObject(key: string): Promise<void> {
  try {
    await getClient().send(
      new DeleteObjectCommand({ Bucket: config.storage.bucket, Key: key })
    );
  } catch (error) {
    logger.error({ error, key }, 'B2 delete failed');
    throw new StorageError('Failed to delete file from storage');
  }
}

export async function headObject(key: string): Promise<{ size: number; contentType?: string } | null> {
  try {
    const response = await getClient().send(
      new HeadObjectCommand({ Bucket: config.storage.bucket, Key: key })
    );
    return { size: response.ContentLength ?? 0, contentType: response.ContentType };
  } catch (error: any) {
    if (error?.$metadata?.httpStatusCode === 404 || error?.name === 'NotFound') {
      return null;
    }
    logger.error({ error, key }, 'B2 head failed');
    throw new StorageError('Failed to read file metadata from storage');
  }
}
