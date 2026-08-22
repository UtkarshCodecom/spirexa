'use client';

import { PhotoCard } from './PhotoCard';
import { DateGroup } from './DateGroup';
import { groupMediaByDate } from '@/lib/utils';
import type { MediaItem } from '@/types';

interface PhotoGridProps {
  items: MediaItem[];
  onItemClick: (item: MediaItem) => void;
  selectedIds?: Set<string>;
  onSelect?: (id: string) => void;
}

export function PhotoGrid({ items, onItemClick, selectedIds, onSelect }: PhotoGridProps) {
  const groups = groupMediaByDate(items);

  return (
    <div className="grid grid-cols-2 sm:grid-cols-4 md:grid-cols-6 lg:grid-cols-8 gap-1">
      {groups.map((group) => (
        <div key={group.label} className="contents">
          <DateGroup label={group.label} />
          {group.items.map((item) => (
            <PhotoCard
              key={item.id}
              item={item}
              onClick={() => onItemClick(item)}
              selected={selectedIds?.has(item.id)}
              onSelect={onSelect}
            />
          ))}
        </div>
      ))}
    </div>
  );
}
