'use client';

import Link from 'next/link';
import { Image as ImageIcon } from 'lucide-react';
import type { Album } from '@/types';

interface AlbumCardProps {
  album: Album;
}

export function AlbumCard({ album }: AlbumCardProps) {
  return (
    <Link
      href={`/albums/${album.id}`}
      className="group block overflow-hidden rounded-xl bg-white border border-surface-200 hover:shadow-lg transition-shadow"
    >
      <div className="aspect-square bg-surface-100 overflow-hidden">
        {album.coverMediaId ? (
          <img
            src={`/api/media/${album.coverMediaId}/thumbnail`}
            alt={album.name}
            className="h-full w-full object-cover transition-transform group-hover:scale-105"
          />
        ) : (
          <div className="flex h-full items-center justify-center">
            <ImageIcon className="h-12 w-12 text-surface-300" />
          </div>
        )}
      </div>
      <div className="p-3">
        <h3 className="font-medium text-surface-900 truncate">{album.name}</h3>
        <p className="text-sm text-surface-500">
          {album.mediaCount} photo{album.mediaCount !== 1 ? 's' : ''}
        </p>
      </div>
    </Link>
  );
}
