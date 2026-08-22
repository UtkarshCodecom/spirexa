'use client';

import { useState } from 'react';
import { useMediaList } from '@/hooks/useMedia';
import { PhotoGrid } from '@/components/photos/PhotoGrid';
import { PhotoViewer } from '@/components/photos/PhotoViewer';
import { EmptyState } from '@/components/photos/EmptyState';
import { Spinner } from '@/components/ui/Spinner';
import type { MediaItem } from '@/types';

export default function FavoritesPage() {
  const [viewerIndex, setViewerIndex] = useState<number | null>(null);
  const { data, loading } = useMediaList({ favorite: true, limit: 100 });

  const handleItemClick = (item: MediaItem) => {
    const index = data.findIndex((i) => i.id === item.id);
    setViewerIndex(index);
  };

  if (loading) {
    return (
      <div className="flex h-64 items-center justify-center">
        <Spinner size="lg" />
      </div>
    );
  }

  return (
    <div>
      <h1 className="text-2xl font-bold text-surface-900 mb-6">Favorites</h1>

      {data.length === 0 ? (
        <EmptyState
          title="No favorites yet"
          description="Mark photos as favorites to see them here"
        />
      ) : (
        <PhotoGrid items={data} onItemClick={handleItemClick} />
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
