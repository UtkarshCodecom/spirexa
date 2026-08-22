'use client';

import { useState, useEffect, useCallback } from 'react';
import { X, ChevronLeft, ChevronRight, Heart, Download, Trash2, Info, FolderPlus } from 'lucide-react';
import { cn } from '@/lib/utils';
import { mediaUrl } from '@/lib/api';
import { useKeyboard } from '@/hooks/useKeyboard';
import { useFavorite } from '@/hooks/useMedia';
import type { MediaItem } from '@/types';

interface PhotoViewerProps {
  items: MediaItem[];
  currentIndex: number;
  onClose: () => void;
  onNavigate: (index: number) => void;
}

export function PhotoViewer({ items, currentIndex, onClose, onNavigate }: PhotoViewerProps) {
  const [showInfo, setShowInfo] = useState(false);
  const [zoom, setZoom] = useState(1);
  const { toggle } = useFavorite();
  const currentItem = items[currentIndex];

  const goNext = useCallback(() => {
    if (currentIndex < items.length - 1) {
      onNavigate(currentIndex + 1);
      setZoom(1);
    }
  }, [currentIndex, items.length, onNavigate]);

  const goPrev = useCallback(() => {
    if (currentIndex > 0) {
      onNavigate(currentIndex - 1);
      setZoom(1);
    }
  }, [currentIndex, onNavigate]);

  useKeyboard({
    onLeft: goPrev,
    onRight: goNext,
    onEscape: onClose,
  });

  useEffect(() => {
    setZoom(1);
  }, [currentIndex]);

  const handleWheel = (e: React.WheelEvent) => {
    if (e.ctrlKey || e.metaKey) {
      e.preventDefault();
      setZoom((z) => Math.min(Math.max(z + (e.deltaY > 0 ? -0.1 : 0.1), 0.5), 3));
    }
  };

  const handleDownload = async () => {
    if (!currentItem) return;
    const a = document.createElement('a');
    a.href = mediaUrl(currentItem.storageUrl);
    a.download = currentItem.fileName;
    a.click();
  };

  const handleFavorite = async () => {
    if (!currentItem) return;
    await toggle(currentItem.id, currentItem.favorite);
  };

  if (!currentItem) return null;

  return (
    <div className="fixed inset-0 z-50 flex bg-black">
      <button
        onClick={onClose}
        className="absolute top-4 left-4 z-10 rounded-full bg-black/50 p-2 text-white hover:bg-black/70"
      >
        <X className="h-5 w-5" />
      </button>

      <div className="absolute top-4 right-4 z-10 flex items-center gap-2">
        <button
          onClick={() => setShowInfo(!showInfo)}
          className={cn(
            'rounded-full p-2 text-white hover:bg-white/20',
            showInfo && 'bg-white/20'
          )}
        >
          <Info className="h-5 w-5" />
        </button>
        <button onClick={handleFavorite} className="rounded-full p-2 text-white hover:bg-white/20">
          <Heart
            className="h-5 w-5"
            fill={currentItem.favorite ? 'currentColor' : 'none'}
            color={currentItem.favorite ? '#ef4444' : 'currentColor'}
          />
        </button>
        <button onClick={handleDownload} className="rounded-full p-2 text-white hover:bg-white/20">
          <Download className="h-5 w-5" />
        </button>
        <button className="rounded-full p-2 text-white hover:bg-white/20">
          <FolderPlus className="h-5 w-5" />
        </button>
        <button className="rounded-full p-2 text-white hover:bg-white/20">
          <Trash2 className="h-5 w-5" />
        </button>
      </div>

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

      <div
        className="flex-1 flex items-center justify-center overflow-hidden"
        onWheel={handleWheel}
      >
        <img
          src={mediaUrl(currentItem.storageUrl)}
          alt={currentItem.fileName}
          className="max-h-full max-w-full object-contain transition-transform"
          style={{ transform: `scale(${zoom})` }}
        />
      </div>

      <div className="absolute bottom-4 left-1/2 -translate-x-1/2 text-white text-sm bg-black/50 rounded-full px-3 py-1">
        {currentIndex + 1} / {items.length}
      </div>

      {showInfo && (
        <div className="absolute right-0 top-0 bottom-0 w-80 bg-surface-900 text-white p-6 overflow-y-auto">
          <h3 className="text-lg font-semibold mb-4">Info</h3>
          <dl className="space-y-3 text-sm">
            <div>
              <dt className="text-surface-400">Filename</dt>
              <dd className="mt-1">{currentItem.fileName}</dd>
            </div>
            <div>
              <dt className="text-surface-400">Date</dt>
              <dd className="mt-1">
                {new Date(currentItem.createdAt).toLocaleDateString('en-US', {
                  year: 'numeric',
                  month: 'long',
                  day: 'numeric',
                })}
              </dd>
            </div>
            <div>
              <dt className="text-surface-400">Dimensions</dt>
              <dd className="mt-1">
                {currentItem.width} x {currentItem.height}
              </dd>
            </div>
            <div>
              <dt className="text-surface-400">Size</dt>
              <dd className="mt-1">{(currentItem.size / 1024 / 1024).toFixed(1)} MB</dd>
            </div>
            <div>
              <dt className="text-surface-400">Type</dt>
              <dd className="mt-1">{currentItem.mimeType}</dd>
            </div>
          </dl>
        </div>
      )}
    </div>
  );
}
