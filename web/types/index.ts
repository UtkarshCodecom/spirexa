export type { User, Media, Album, MediaType, PaginatedResponse, ApiResponse, ApiError, SearchQuery } from '@photos/types';

export interface MediaMetadata {
  dateTaken?: string;
  location?: { latitude: number; longitude: number };
  /** AI-generated search caption — null/absent until background analysis finishes. */
  aiCaption?: string;
  aiTags?: string[];
  isDocument?: boolean;
  documentText?: string;
}

export interface MediaItem {
  id: string;
  ownerId: string;
  fileName: string;
  mimeType: string;
  width: number | null;
  height: number | null;
  size: number;
  storageUrl: string;
  favorite: boolean;
  deleted: boolean;
  deletedAt: string | null;
  createdAt: string;
  /** When the photo was actually taken, falling back to upload time — always present. */
  takenAt: string;
  updatedAt: string;
  type: 'image' | 'video';
  metadata?: MediaMetadata;
}

export interface AlbumDetail {
  id: string;
  ownerId: string;
  name: string;
  description: string | null;
  coverMediaId: string | null;
  mediaCount: number;
  createdAt: string;
  updatedAt: string;
  media: MediaItem[];
}

export interface AuthContextType {
  user: import('firebase/auth').User | null;
  loading: boolean;
  signIn: (email: string, password: string) => Promise<void>;
  signUp: (email: string, password: string) => Promise<void>;
  signInWithGoogle: () => Promise<void>;
  signOut: () => Promise<void>;
}

export interface NavItem {
  label: string;
  href: string;
  icon: string;
}

export interface UploadProgress {
  id: string;
  file: File;
  progress: number;
  status: 'pending' | 'uploading' | 'completed' | 'error';
  error?: string;
}

export interface MediaFilters {
  limit?: number;
  cursor?: string;
  type?: 'all' | 'images' | 'videos';
  favorite?: boolean;
  deleted?: boolean;
  search?: string;
  startDate?: string;
  endDate?: string;
}
