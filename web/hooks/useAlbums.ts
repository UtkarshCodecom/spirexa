'use client';

import { useState, useEffect, useCallback } from 'react';
import { apiClient } from '@/lib/api';
import type { Album, AlbumDetail } from '@/types';

export function useAlbumList() {
  const [data, setData] = useState<Album[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchAlbums = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const albums = await apiClient.get<Album[]>('/api/albums');
      setData(albums);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to fetch albums');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchAlbums();
  }, [fetchAlbums]);

  return { data, loading, error, refetch: fetchAlbums };
}

export function useAlbum(id: string) {
  const [data, setData] = useState<AlbumDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!id) return;
    setLoading(true);
    apiClient
      .get<AlbumDetail>(`/api/albums/${id}`)
      .then(setData)
      .catch((err) => setError(err instanceof Error ? err.message : 'Failed to fetch album'))
      .finally(() => setLoading(false));
  }, [id]);

  return { data, loading, error };
}

export function useCreateAlbum() {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const create = useCallback(async (name: string, description?: string) => {
    setLoading(true);
    setError(null);
    try {
      const album = await apiClient.post<Album>('/api/albums', { name, description });
      return album;
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to create album');
      throw err;
    } finally {
      setLoading(false);
    }
  }, []);

  return { create, loading, error };
}

export function useUpdateAlbum() {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const update = useCallback(async (id: string, data: { name?: string; description?: string }) => {
    setLoading(true);
    setError(null);
    try {
      const album = await apiClient.patch<Album>(`/api/albums/${id}`, data);
      return album;
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to update album');
      throw err;
    } finally {
      setLoading(false);
    }
  }, []);

  return { update, loading, error };
}

export function useDeleteAlbum() {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const deleteAlbum = useCallback(async (id: string) => {
    setLoading(true);
    setError(null);
    try {
      await apiClient.delete(`/api/albums/${id}`);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to delete album');
      throw err;
    } finally {
      setLoading(false);
    }
  }, []);

  return { delete: deleteAlbum, loading, error };
}

export function useAddToAlbum() {
  const [loading, setLoading] = useState(false);

  const addToAlbum = useCallback(async (albumId: string, mediaIds: string[]) => {
    setLoading(true);
    try {
      await apiClient.post(`/api/albums/${albumId}/photos`, { mediaIds });
    } finally {
      setLoading(false);
    }
  }, []);

  return { addToAlbum, loading };
}

export function useRemoveFromAlbum() {
  const [loading, setLoading] = useState(false);

  const removeFromAlbum = useCallback(async (albumId: string, mediaId: string) => {
    setLoading(true);
    try {
      await apiClient.delete(`/api/albums/${albumId}/photos/${mediaId}`);
    } finally {
      setLoading(false);
    }
  }, []);

  return { removeFromAlbum, loading };
}
