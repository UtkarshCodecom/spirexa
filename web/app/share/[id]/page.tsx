'use client';

import { useEffect, useState, useCallback } from 'react';
import { useParams } from 'next/navigation';
import { X, ChevronLeft, ChevronRight, Play, ImageOff } from 'lucide-react';
import { apiClient, mediaUrl } from '@/lib/api';
import { groupMediaByDate } from '@/lib/utils';
import { useKeyboard } from '@/hooks/useKeyboard';
import { Spinner } from '@/components/ui/Spinner';
import type { MediaItem } from '@/types';

interface SharePublicView {
  id: string;
  title: string;
  createdAt: string;
  media: MediaItem[];
}

/**
 * Public, read-only view of a shared album — no sign-in required. Anyone
 * with this link can open it, whether or not they have a Photos account.
 * Deliberately does NOT reuse PhotoCard/PhotoViewer: those bake in
 * authenticated actions (favorite, delete, download-via-auth) that would
 * 401 and bounce an anonymous visitor to /login the moment they're touched.
 */
export default function SharePage() {
  const params = useParams<{ id: string }>();
  const [share, setShare] = useState<SharePublicView | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [viewerIndex, setViewerIndex] = useState<number | null>(null);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError(null);
    apiClient
      .get<SharePublicView>(`/api/shares/${params.id}`)
      .then((data) => {
        if (!cancelled) setShare(data);
      })
      .catch((err) => {
        if (!cancelled) setError(err instanceof Error ? err.message : 'This link isn’t available');
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [params.id]);

  if (loading) {
    return (
      <div className="flex h-screen items-center justify-center bg-surface-50">
        <Spinner size="lg" />
      </div>
    );
  }

  if (error || !share) {
    return (
      <div className="flex h-screen flex-col items-center justify-center gap-3 bg-surface-50 px-6 text-center">
        <ImageOff className="h-10 w-10 text-surface-400" />
        <h1 className="text-lg font-semibold text-surface-900">Link unavailable</h1>
        <p className="text-sm text-surface-500">{error ?? 'This shared album could not be found.'}</p>
      </div>
    );
  }

  const groups = groupMediaByDate(share.media);

  return (
    <div className="min-h-screen bg-surface-50">
      <header className="border-b border-surface-200 bg-white px-6 py-5">
        <h1 className="text-2xl font-bold text-surface-900">{share.title}</h1>
        <p className="mt-1 text-sm text-surface-500">
          {share.media.length} item{share.media.length === 1 ? '' : 's'} shared with you — no account needed
        </p>
      </header>

      <main className="mx-auto max-w-6xl px-4 py-6">
        {share.media.length === 0 ? (
          <p className="py-16 text-center text-surface-500">Nothing to show here.</p>
        ) : (
          <div className="grid grid-cols-2 sm:grid-cols-4 md:grid-cols-6 gap-1">
            {groups.map((group) => (
              <div key={group.label} className="contents">
                <div className="col-span-full flex items-center gap-4 py-4">
                  <h2 className="text-lg font-semibold text-surface-900 whitespace-nowrap">{group.label}</h2>
                  <div className="h-px flex-1 bg-surface-200" />
                </div>
                {group.items.map((item) => (
                  <SharedCard
                    key={item.id}
                    item={item}
                    onClick={() => setViewerIndex(share.media.findIndex((m) => m.id === item.id))}
                  />
                ))}
              </div>
            ))}
          </div>
        )}
      </main>

      {viewerIndex !== null && (
        <SharedViewer
          items={share.media}
          currentIndex={viewerIndex}
          onClose={() => setViewerIndex(null)}
          onNavigate={setViewerIndex}
        />
      )}
    </div>
  );
}

function SharedCard({ item, onClick }: { item: MediaItem; onClick: () => void }) {
  const isVideo = item.mimeType.startsWith('video/');
  return (
    <button
      onClick={onClick}
      className="group relative aspect-square overflow-hidden rounded-lg bg-surface-100"
    >
      {!isVideo && (
        <img
          src={mediaUrl(item.storageUrl)}
          alt={item.fileName}
          loading="lazy"
          className="h-full w-full object-cover transition-transform duration-200 group-hover:scale-105"
        />
      )}
      {isVideo && (
        <div className="flex h-full w-full items-center justify-center bg-surface-800">
          <Play className="h-8 w-8 text-white/90" fill="currentColor" />
        </div>
      )}
    </button>
  );
}

function SharedViewer({
  items,
  currentIndex,
  onClose,
  onNavigate,
}: {
  items: MediaItem[];
  currentIndex: number;
  onClose: () => void;
  onNavigate: (index: number) => void;
}) {
  const current = items[currentIndex];
  const isVideo = current?.mimeType.startsWith('video/');

  const goNext = useCallback(() => {
    if (currentIndex < items.length - 1) onNavigate(currentIndex + 1);
  }, [currentIndex, items.length, onNavigate]);

  const goPrev = useCallback(() => {
    if (currentIndex > 0) onNavigate(currentIndex - 1);
  }, [currentIndex, onNavigate]);

  useKeyboard({ onLeft: goPrev, onRight: goNext, onEscape: onClose });

  if (!current) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black">
      <button
        onClick={onClose}
        className="absolute top-4 left-4 z-10 rounded-full bg-black/50 p-2 text-white hover:bg-black/70"
      >
        <X className="h-5 w-5" />
      </button>

      {currentIndex > 0 && (
        <button
          onClick={goPrev}
          className="absolute left-4 top-1/2 z-10 -translate-y-1/2 rounded-full bg-black/50 p-2 text-white hover:bg-black/70"
        >
          <ChevronLeft className="h-6 w-6" />
        </button>
      )}
      {currentIndex < items.length - 1 && (
        <button
          onClick={goNext}
          className="absolute right-4 top-1/2 z-10 -translate-y-1/2 rounded-full bg-black/50 p-2 text-white hover:bg-black/70"
        >
          <ChevronRight className="h-6 w-6" />
        </button>
      )}

      <div className="flex h-full w-full items-center justify-center overflow-hidden p-8">
        {isVideo ? (
          <video src={mediaUrl(current.storageUrl)} controls autoPlay className="max-h-full max-w-full" />
        ) : (
          <img src={mediaUrl(current.storageUrl)} alt={current.fileName} className="max-h-full max-w-full object-contain" />
        )}
      </div>

      <div className="absolute bottom-4 left-1/2 -translate-x-1/2 rounded-full bg-black/50 px-3 py-1 text-sm text-white">
        {currentIndex + 1} / {items.length}
      </div>
    </div>
  );
}
