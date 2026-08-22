'use client';

import { useState } from 'react';
import { Link2, X } from 'lucide-react';
import { useMediaList } from '@/hooks/useMedia';
import { useInfiniteScroll } from '@/hooks/useInfiniteScroll';
import { apiClient } from '@/lib/api';
import { useToast } from '@/components/ui/Toast';
import { PhotoGrid } from '@/components/photos/PhotoGrid';
import { PhotoViewer } from '@/components/photos/PhotoViewer';
import { EmptyState } from '@/components/photos/EmptyState';
import { UploadButton } from '@/components/upload/UploadButton';
import { Spinner } from '@/components/ui/Spinner';
import type { MediaItem } from '@/types';

interface ShareResponse {
  id: string;
  title: string;
  mediaCount: number;
  shareUrl: string;
}

export default function PhotosPage() {
  const [viewerIndex, setViewerIndex] = useState<number | null>(null);
  const [selectedIds, setSelectedIds] = useState<Set<string>>(new Set());
  const [sharing, setSharing] = useState(false);
  const { data, loading, hasMore, loadMore } = useMediaList({ limit: 50 });
  const sentinelRef = useInfiniteScroll(loadMore, hasMore, loading);
  const { addToast } = useToast();

  const handleItemClick = (item: MediaItem) => {
    if (selectedIds.size > 0) {
      toggleSelect(item.id);
      return;
    }
    const index = data.findIndex((i) => i.id === item.id);
    setViewerIndex(index);
  };

  const toggleSelect = (id: string) => {
    setSelectedIds((prev) => {
      const next = new Set(prev);
      if (!next.delete(id)) next.add(id);
      return next;
    });
  };

  const handleShareLink = async () => {
    const title = window.prompt('Title for this shared album:', 'Shared photos');
    if (title === null) return; // cancelled
    setSharing(true);
    try {
      const share = await apiClient.post<ShareResponse>('/api/shares', {
        title: title.trim() || 'Shared photos',
        mediaIds: Array.from(selectedIds),
      });
      await navigator.clipboard.writeText(share.shareUrl).catch(() => undefined);
      addToast('success', `Link copied — anyone can view ${share.mediaCount} item(s), no account needed`);
      setSelectedIds(new Set());
    } catch (err) {
      addToast('error', err instanceof Error ? err.message : 'Could not create the share link');
    } finally {
      setSharing(false);
    }
  };

  if (loading && data.length === 0) {
    return (
      <div className="flex h-64 items-center justify-center">
        <Spinner size="lg" />
      </div>
    );
  }

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold text-surface-900">Photos</h1>
        <div className="flex items-center gap-2">
          {selectedIds.size > 0 && (
            <>
              <button
                onClick={handleShareLink}
                disabled={sharing}
                className="flex items-center gap-2 rounded-lg bg-primary-600 px-4 py-2 text-sm font-medium text-white hover:bg-primary-700 disabled:opacity-60"
              >
                <Link2 className="h-4 w-4" />
                Share {selectedIds.size} as link
              </button>
              <button
                onClick={() => setSelectedIds(new Set())}
                className="flex items-center gap-1 rounded-lg px-3 py-2 text-sm text-surface-600 hover:bg-surface-100"
              >
                <X className="h-4 w-4" />
                Clear
              </button>
            </>
          )}
          <UploadButton />
        </div>
      </div>

      {data.length === 0 ? (
        <EmptyState
          title="No photos yet"
          description="Upload your first photo to get started"
          actionLabel="Upload photos"
        />
      ) : (
        <>
          <PhotoGrid
            items={data}
            onItemClick={handleItemClick}
            selectedIds={selectedIds}
            onSelect={toggleSelect}
          />
          <div ref={sentinelRef} className="h-4" />
          {loading && (
            <div className="flex justify-center py-8">
              <Spinner />
            </div>
          )}
        </>
      )}

      {viewerIndex !== null && (
        <PhotoViewer
          items={data}
          currentIndex={viewerIndex}
          onClose={() => setViewerIndex(null)}
          onNavigate={setViewerIndex}
        />
      )}
    </div>
  );
}
