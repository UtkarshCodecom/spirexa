import { auth } from '@/lib/firebase';

const BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:3001';

/**
 * The server returns media content URLs as relative paths (e.g.
 * "/api/media/{id}/content?token=..." — see media.service.ts's
 * mapToMediaItem) since it doesn't know its own public origin. Used directly
 * in an <img>/<video> src, a relative path resolves against the WEB app's
 * own origin (port 3000) instead of the API server's (port 3002), which
 * 404s — this is the one place that turns it into an absolute URL. Always
 * route storageUrl/contentPath values through this before rendering them.
 */
export function mediaUrl(path: string): string {
  if (!path) return path;
  return path.startsWith('http://') || path.startsWith('https://') ? path : `${BASE_URL}${path}`;
}

interface RequestOptions {
  method?: string;
  body?: unknown;
  headers?: Record<string, string>;
}

async function getIdToken(): Promise<string | null> {
  const user = auth.currentUser;
  if (!user) return null;
  return user.getIdToken(true);
}

async function request<T = unknown>(endpoint: string, options: RequestOptions = {}): Promise<T> {
  const { method = 'GET', body, headers = {} } = options;

  const token = await getIdToken();

  const config: RequestInit = {
    method,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...headers,
    },
  };

  if (body) {
    config.body = JSON.stringify(body);
  }

  const response = await fetch(`${BASE_URL}${endpoint}`, config);

  if (response.status === 401) {
    window.location.href = '/login';
    throw new Error('Unauthorized');
  }

  if (response.status === 403) {
    throw new Error('Forbidden');
  }

  if (response.status === 404) {
    throw new Error('Not found');
  }

  if (response.status === 429) {
    throw new Error('Too many requests');
  }

  if (!response.ok) {
    throw new Error(`Request failed with status ${response.status}`);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  // Every server response wraps its real payload as { success, data } (see
  // server/src/utils/response.ts's sendSuccess) — unwrap it here, once,
  // so every caller of apiClient.get/post/patch/delete gets the payload
  // directly, matching what their type parameters already claim.
  const envelope = await response.json();
  return envelope.data as T;
}

/**
 * Uploads a file in one call — streamed straight through to storage server-side.
 * No Content-Type header here: the browser sets multipart/form-data with the
 * correct boundary itself when given a FormData body.
 */
async function upload<T = unknown>(file: File): Promise<T> {
  const token = await getIdToken();
  const form = new FormData();
  form.append('fileName', file.name);
  form.append('mimeType', file.type || 'application/octet-stream');
  form.append('size', String(file.size));
  form.append('file', file, file.name);

  const response = await fetch(`${BASE_URL}/api/media/upload`, {
    method: 'POST',
    headers: token ? { Authorization: `Bearer ${token}` } : {},
    body: form,
  });

  if (response.status === 401) {
    window.location.href = '/login';
    throw new Error('Unauthorized');
  }
  if (!response.ok) {
    throw new Error(`Upload failed with status ${response.status}`);
  }
  const envelope = await response.json();
  return envelope.data as T;
}

export const apiClient = {
  get: <T = unknown>(endpoint: string) => request<T>(endpoint),
  post: <T = unknown>(endpoint: string, body?: unknown) =>
    request<T>(endpoint, { method: 'POST', body }),
  patch: <T = unknown>(endpoint: string, body?: unknown) =>
    request<T>(endpoint, { method: 'PATCH', body }),
  delete: <T = unknown>(endpoint: string) => request<T>(endpoint, { method: 'DELETE' }),
  upload,
};
