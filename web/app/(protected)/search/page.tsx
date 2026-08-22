'use client';

import { useState } from 'react';
import { useSearchParams } from 'next/navigation';
import { useMediaList } from '@/hooks/useMedia';
import { PhotoGrid } from '@/components/photos/PhotoGrid';
import { PhotoViewer } from '@/components/photos/PhotoViewer';
import { EmptyState } from '@/components/photos/EmptyState';
import { Input } from '@/components/ui/Input';
import { Spinner } from '@/components/ui/Spinner';
import { Search } from 'lucide-react';
import type { MediaItem } from '@/types';

export default function SearchPage() {
  const searchParams = useSearchParams();
  const initialQuery = searchParams.get('q') || '';
  const [query, setQuery] = useState(initialQuery);
  const [searchQuery, setSearchQuery] = useState(initialQuery);
  const [viewerIndex, setViewerIndex] = useState<number | null>(null);
  const { data, loading } = useMediaList({
    search: searchQuery,
    limit: 100,
  });

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    setSearchQuery(query);
  };

  const handleItemClick = (item: MediaItem) => {
    const index = data.findIndex((i) => i.id === item.id);
    setViewerIndex(index);
  };

  return (
    <div>
      <h1 className="text-2xl font-bold text-surface-900 mb-6">Search</h1>

      <form onSubmit={handleSearch} className="mb-6 max-w-xl">
        <Input
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          placeholder="Search photos..."
          icon={<Search className="h-4 w-4" />}
        />
      </form>

      {loading ? (
        <div className="flex h-64 items-center justify-center">
          <Spinner size="lg" />
        </div>
      ) : searchQuery && data.length === 0 ? (
        <EmptyState
          title="No results found"
          description={`No photos match "${searchQuery}"`}
        />
      ) : !searchQuery ? (
        <EmptyState
          title="Search your photos"
          description="Type something to search through your photos"
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
