export enum MediaType {
  IMAGE = 'image',
  VIDEO = 'video',
}

export enum UploadStatus {
  NOT_UPLOADED = 'NOT_UPLOADED',
  PENDING = 'PENDING',
  UPLOADING = 'UPLOADING',
  UPLOADED = 'UPLOADED',
  FAILED = 'FAILED',
  SKIPPED = 'SKIPPED',
  EXCLUDED = 'EXCLUDED',
}

export interface User {
  uid: string;
  displayName: string;
  email: string;
  photoURL: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface Media {
  id: string;
  ownerId: string;
  type: MediaType;
  fileName: string;
  storagePath: string;
  storageUrl: string;
  mimeType: string;
  size: number;
  width: number | null;
  height: number | null;
  createdAt: string;
  takenAt: string | null;
  updatedAt: string;
  favorite: boolean;
  deleted: boolean;
  deletedAt: string | null;
  metadata: Record<string, unknown>;
}

export interface Album {
  id: string;
  ownerId: string;
  name: string;
  description: string | null;
  coverMediaId: string | null;
  mediaCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface AlbumMedia {
  addedAt: string;
  order: number | null;
}

export interface PaginationCursor {
  value: string;
  field: string;
}

export interface PaginatedResponse<T> {
  items: T[];
  nextCursor: string | null;
  hasMore: boolean;
  total: number;
}

export interface ApiResponse<T> {
  success: boolean;
  data: T | null;
  error: ApiError | null;
}

export interface ApiError {
  code: string;
  message: string;
  details?: Record<string, unknown>;
}

export interface UploadUrlRequest {
  fileName: string;
  mimeType: string;
  size: number;
}

export interface UploadUrlResponse {
  uploadUrl: string;
  storagePath: string;
  mediaId: string;
  headers: Record<string, string>;
}

export interface UploadCompleteRequest {
  mediaId: string;
  storagePath: string;
  fileName: string;
  mimeType: string;
  size: number;
  width?: number;
  height?: number;
  takenAt?: string;
}

export interface SearchQuery {
  q?: string;
  type?: MediaType;
  favorite?: boolean;
  albumId?: string;
  startDate?: string;
  endDate?: string;
  limit?: number;
  cursor?: string;
}

export interface BackupDoc {
  signature: string;
  name: string;
  remotePath: string | null;
  size: number;
  isVideo: boolean;
  dateSec: number;
  excluded: boolean;
}
