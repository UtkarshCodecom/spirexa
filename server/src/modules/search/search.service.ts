import { db } from '../../config/firebase';
import { PaginationResult } from '../../types';
import { signContentToken } from '../../utils/contentToken';

export interface SearchResult {
  id: string;
  ownerId: string;
  fileName: string;
  mimeType: string;
  size: number;
  storageUrl: string;
  description?: string;
  favorite: boolean;
  createdAt: Date;
  type: 'media' | 'album';
}

export interface SearchFilters {
  q?: string;
  type?: 'all' | 'photos' | 'videos';
  favorite?: boolean;
  startDate?: string;
  endDate?: string;
  limit?: number;
  cursor?: string;
}

const PHOTO_MIME_TYPES = [
  'image/jpeg',
  'image/png',
  'image/gif',
  'image/webp',
  'image/heic',
  'image/heif',
];

const VIDEO_MIME_TYPES = [
  'video/mp4',
  'video/quicktime',
  'video/x-msvideo',
  'video/webm',
];

export async function search(
  uid: string,
  filters: SearchFilters
): Promise<PaginationResult<SearchResult>> {
  const limit = filters.limit || 20;
  const mediaCollection = db
    .collection('users')
    .doc(uid)
    .collection('media');

  let query = mediaCollection.where('deleted', '==', false);

  // Filter by type
  if (filters.type === 'photos') {
    query = query.where('mimeType', 'in', PHOTO_MIME_TYPES);
  } else if (filters.type === 'videos') {
    query = query.where('mimeType', 'in', VIDEO_MIME_TYPES);
  }

  // Filter by favorite
  if (filters.favorite !== undefined) {
    query = query.where('favorite', '==', filters.favorite);
  }

  // Filter by date range
  if (filters.startDate) {
    query = query.where('createdAt', '>=', new Date(filters.startDate));
  }

  if (filters.endDate) {
    query = query.where('createdAt', '<=', new Date(filters.endDate));
  }

  // Order by creation date
  query = query.orderBy('createdAt', 'desc').limit(limit + 1);

  // Apply cursor
  if (filters.cursor) {
    const cursorDoc = await mediaCollection.doc(filters.cursor).get();
    if (cursorDoc.exists) {
      query = query.startAfter(cursorDoc);
    }
  }

  const snapshot = await query.get();
  const results: SearchResult[] = [];

  snapshot.forEach((doc) => {
    const data = doc.data();
    results.push({
      id: doc.id,
      ownerId: data.ownerId,
      fileName: data.fileName,
      mimeType: data.mimeType,
      size: data.size,
      storageUrl: `/api/media/${doc.id}/content?token=${signContentToken(uid, doc.id)}`,
      description: data.description,
      favorite: data.favorite || false,
      createdAt: data.createdAt?.toDate?.() || new Date(data.createdAt),
      type: 'media',
    });
  });

  let nextCursor: string | undefined;
  if (results.length > limit) {
    const lastItem = results.pop();
    nextCursor = lastItem?.id;
  }

  // If there's a text query, filter results in-memory
  // This is a simple implementation; a production system would use
  // a dedicated search engine like Algolia or Elasticsearch
  let filteredResults = results;

  if (filters.q) {
    const queryLower = filters.q.toLowerCase();
    filteredResults = results.filter(
      (r) =>
        r.fileName.toLowerCase().includes(queryLower) ||
        (r.description && r.description.toLowerCase().includes(queryLower))
    );
  }

  return {
    data: filteredResults,
    nextCursor,
    hasMore: !!nextCursor,
  };
}
