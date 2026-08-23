'use client';

import { useState } from 'react';
import { FileText, Heart } from 'lucide-react';
import { cn } from '@/lib/utils';
import { mediaUrl } from '@/lib/api';
import { useFavorite } from '@/hooks/useMedia';
import type { MediaItem } from '@/types';

interface PhotoCardProps {
  item: MediaItem;
  onClick: () => void;
  selected?: boolean;
  onSelect?: (id: string) => void;
}

export function PhotoCard({ item, onClick, selected, onSelect }: PhotoCardProps) {
  const [isHovered, setIsHovered] = useState(false);
  const { toggle } = useFavorite();

  const handleFavorite = async (e: React.MouseEvent) => {
    e.stopPropagation();
    await toggle(item.id, item.favorite);
  };

  const handleSelect = (e: React.MouseEvent) => {
    e.stopPropagation();
    onSelect?.(item.id);
  };

  return (
    <div
      className="group relative aspect-square cursor-pointer overflow-hidden rounded-lg bg-surface-100"
      onMouseEnter={() => setIsHovered(true)}
      onMouseLeave={() => setIsHovered(false)}
      onClick={onClick}
    >
      <img
        src={mediaUrl(item.storageUrl)}
        alt={item.metadata?.aiCaption || item.fileName}
        loading="lazy"
        className="h-full w-full object-cover transition-transform duration-200 group-hover:scale-105"
      />

      {item.metadata?.isDocument && !isHovered && (
        <div className="absolute top-2 left-2 rounded-full bg-black/60 p-1">
          <FileText className="h-3 w-3 text-white" />
        </div>
      )}

      {(isHovered || selected) && (
        <div className="absolute inset-0 bg-black/20 transition-opacity" />
      )}

      {isHovered && (
        <>
          <button
            onClick={handleSelect}
            className={cn(
              'absolute top-2 left-2 h-6 w-6 rounded-full border-2 flex items-center justify-center transition-colors',
              selected
                ? 'bg-primary-600 border-primary-600 text-white'
                : 'bg-white/80 border-white/80 text-surface-600 hover:bg-white'
            )}
          >
            {selected && (
              <svg className="h-3 w-3" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={3} d="M5 13l4 4L19 7" />
              </svg>
            )}
          </button>

          <button
            onClick={handleFavorite}
            className={cn(
              'absolute top-2 right-2 h-8 w-8 rounded-full flex items-center justify-center transition-colors',
              item.favorite
                ? 'bg-red-500 text-white'
                : 'bg-white/80 text-surface-600 hover:bg-white hover:text-red-500'
            )}
          >
            <Heart className="h-4 w-4" fill={item.favorite ? 'currentColor' : 'none'} />
          </button>
        </>
      )}

      {item.favorite && !isHovered && (
        <div className="absolute top-2 right-2">
          <Heart className="h-4 w-4 text-white drop-shadow" fill="currentColor" />
        </div>
      )}
    </div>
  );
}
