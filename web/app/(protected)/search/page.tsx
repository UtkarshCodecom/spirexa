'use client';

import { useMemo, useState } from 'react';
import { FileText, Search } from 'lucide-react';
import { useMediaList } from '@/hooks/useMedia';
import { PhotoGrid } from '@/components/photos/PhotoGrid';
import { PhotoViewer } from '@/components/photos/PhotoViewer';
import { EmptyState } from '@/components/photos/EmptyState';
import { Input } from '@/components/ui/Input';
import { Spinner } from '@/components/ui/Spinner';
import { cn } from '@/lib/utils';
import type { MediaItem } from '@/types';

/**
 * AI-powered search — matches an AI-generated caption/tags for each photo,
 * not just the filename (see server/src/services/ai.service.ts). Filtering
 * happens entirely client-side over the already-fetched list, mirroring the
 * Android app's SearchScreen: the AI work already happened once at upload
 * time, so typing a query here costs nothing extra.
 */
export default function SearchPage() {
  const [query, setQuery] = useState('');
  const [documentsOnly, setDocumentsOnly] = useState(false);
  const [viewerIndex, setViewerIndex] = useState<number | null>(null);
  // One large page covers a personal library well enough for search purposes
  // without adding a second, separate fetch path — the server caps at 200.
  const { data, loading, refetch } = useMediaList({ limit: 200 });

  const results = useMemo(() => {
    const base = data.filter((item) => !item.deleted && (!documentsOnly || item.metadata?.isDocument));
    const needle = query.trim().toLowerCase();
    if (!needle) return documentsOnly ? base : [];
    return base.filter((item) => {
      const caption = item.metadata?.aiCaption?.toLowerCase();
      const tags = item.metadata?.aiTags ?? [];
      return (
        item.fileName.toLowerCase().includes(needle) ||
        (caption && caption.includes(needle)) ||
        tags.some((tag) => tag.includes(needle))
      );
    });
  }, [data, query, documentsOnly]);

  const handleItemClick = (item: MediaItem) => {
    const index = results.findIndex((i) => i.id === item.id);
    setViewerIndex(index);
  };

  return (
    <div>
      <h1 className="text-2xl font-bold text-surface-900 mb-6">Search</h1>

      <div className="mb-4 max-w-xl">
        <Input
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          placeholder="Search your photos… try “beach”, “receipt”, a color"
          icon={<Search className="h-4 w-4" />}
        />
      </div>

      <div className="mb-6 flex items-center gap-2">
        <button
          onClick={() => setDocumentsOnly(false)}
          className={cn(
            'rounded-full px-4 py-1.5 text-sm font-medium transition-colors',
            !documentsOnly ? 'bg-primary-600 text-white' : 'bg-surface-100 text-surface-600 hover:bg-surface-200'
          )}
        >
          All
        </button>
        <button
          onClick={() => setDocumentsOnly(true)}
          className={cn(
            'flex items-center gap-1.5 rounded-full px-4 py-1.5 text-sm font-medium transition-colors',
            documentsOnly ? 'bg-primary-600 text-white' : 'bg-surface-100 text-surface-600 hover:bg-surface-200'
          )}
        >
          <FileText className="h-3.5 w-3.5" />
          Documents
        </button>
      </div>

      {loading && data.length === 0 ? (
        <div className="flex h-64 items-center justify-center">
          <Spinner size="lg" />
        </div>
      ) : results.length === 0 ? (
        <EmptyState
          title={documentsOnly ? 'No documents found' : query ? 'No matches' : 'Search your photos'}
          description={
            documentsOnly
              ? 'Photos of IDs, receipts, and certificates show up here once analyzed.'
              : query
                ? 'Try different words — search matches what’s in the photo, not just the filename.'
                : 'Type what you remember — a place, an object, a color. Matches what’s actually in the photo.'
          }
        />
      ) : (
        <PhotoGrid items={results} onItemClick={handleItemClick} />
      )}

      {viewerIndex !== null && (
        <PhotoViewer
          items={results}
          currentIndex={viewerIndex}
          onClose={() => setViewerIndex(null)}
          onNavigate={setViewerIndex}
          onDeleted={() => refetch()}
        />
      )}
    </div>
  );
}
