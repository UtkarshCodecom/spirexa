'use client';

import { useState, useEffect, useCallback } from 'react';
import { apiClient } from '@/lib/api';
import type { MediaItem, MediaFilters } from '@/types';

// The shared @photos/types PaginatedResponse (items/total) doesn't match what
// the server actually sends (server/src/types/index.ts's PaginationResult) —
// confirmed by inspecting the raw response directly. This describes reality.
interface MediaListResponse {
  data: MediaItem[];
  nextCursor?: string;
  hasMore: boolean;
}

export function useMediaList(filters: MediaFilters = {}) {
  const [data, setData] = useState<MediaItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [hasMore, setHasMore] = useState(true);

  const fetchMedia = useCallback(async (isLoadMore = false) => {
    setLoading(true);
    setError(null);
    try {
      const params = new URLSearchParams();
      if (filters.cursor) params.set('cursor', filters.cursor);
      if (filters.limit) params.set('limit', String(filters.limit));
      if (filters.type) params.set('type', filters.type);
      if (filters.favorite !== undefined) params.set('favorite', String(filters.favorite));
      if (filters.deleted !== undefined) params.set('deleted', String(filters.deleted));
      if (filters.search) params.set('search', filters.search);
      if (filters.startDate) params.set('startDate', filters.startDate);
      if (filters.endDate) params.set('endDate', filters.endDate);

      const query = params.toString();
      const response = await apiClient.get<MediaListResponse>(
        `/api/media${query ? `?${query}` : ''}`
      );
      setData((prev) => (isLoadMore ? [...prev, ...response.data] : response.data));
      setHasMore(response.hasMore);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to fetch media');
    } finally {
      setLoading(false);
    }
  }, [filters.cursor, filters.limit, filters.type, filters.favorite, filters.deleted, filters.search, filters.startDate, filters.endDate]);

  useEffect(() => {
    fetchMedia(false);
  }, [fetchMedia]);

  const loadMore = useCallback(() => {
    if (hasMore && !loading) {
      fetchMedia(true);
    }
  }, [hasMore, loading, fetchMedia]);

  return { data, loading, error, hasMore, loadMore, refetch: () => fetchMedia(false) };
}

export function useMediaItem(id: string) {
  const [data, setData] = useState<MediaItem | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!id) return;
    setLoading(true);
    apiClient
      .get<MediaItem>(`/api/media/${id}`)
      .then(setData)
      .catch((err) => setError(err instanceof Error ? err.message : 'Failed to fetch media'))
      .finally(() => setLoading(false));
  }, [id]);

  return { data, loading, error };
}

export function useFavorite() {
  const [loading, setLoading] = useState(false);

  const toggle = useCallback(async (id: string, isFavorite: boolean) => {
    setLoading(true);
    try {
      await apiClient.patch(`/api/media/${id}`, { favorite: !isFavorite });
      return !isFavorite;
    } finally {
      setLoading(false);
    }
  }, []);

  return { toggle, loading };
}

export function useTrash() {
  const [loading, setLoading] = useState(false);

  const trash = useCallback(async (id: string) => {
    setLoading(true);
    try {
      await apiClient.patch(`/api/media/${id}`, { deleted: true, deletedAt: new Date().toISOString() });
    } finally {
      setLoading(false);
    }
  }, []);

  return { trash, loading };
}

export function useRestore() {
  const [loading, setLoading] = useState(false);

  const restore = useCallback(async (id: string) => {
    setLoading(true);
    try {
      await apiClient.patch(`/api/media/${id}`, { deleted: false, deletedAt: null });
    } finally {
      setLoading(false);
    }
  }, []);

  return { restore, loading };
}

export function useDelete() {
  const [loading, setLoading] = useState(false);

  const deleteMedia = useCallback(async (id: string) => {
    setLoading(true);
    try {
      await apiClient.delete(`/api/media/${id}`);
    } finally {
      setLoading(false);
    }
  }, []);

  return { delete: deleteMedia, loading };
}
