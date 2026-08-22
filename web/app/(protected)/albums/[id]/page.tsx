'use client';

import { useState } from 'react';
import { useParams, useRouter } from 'next/navigation';
import { useAlbum, useDeleteAlbum } from '@/hooks/useAlbums';
import { PhotoGrid } from '@/components/photos/PhotoGrid';
import { PhotoViewer } from '@/components/photos/PhotoViewer';
import { EmptyState } from '@/components/photos/EmptyState';
import { ConfirmDialog } from '@/components/ui/ConfirmDialog';
import { Spinner } from '@/components/ui/Spinner';
import { Button } from '@/components/ui/Button';
import { Trash2, ArrowLeft } from 'lucide-react';
import { useToast } from '@/components/ui/Toast';
import type { MediaItem } from '@/types';

export default function AlbumDetailPage() {
  const params = useParams();
  const router = useRouter();
  const albumId = params.id as string;
  const { data: album, loading } = useAlbum(albumId);
  const { delete: deleteAlbum } = useDeleteAlbum();
  const { addToast } = useToast();
  const [showDelete, setShowDelete] = useState(false);
  const [viewerIndex, setViewerIndex] = useState<number | null>(null);

  const handleDelete = async () => {
    try {
      await deleteAlbum(albumId);
      addToast('success', 'Album deleted');
      router.push('/albums');
    } catch {
      addToast('error', 'Failed to delete album');
    }
  };

  const handleItemClick = (item: MediaItem) => {
    if (!album?.media) return;
    const index = album.media.findIndex((p) => p.id === item.id);
    setViewerIndex(index);
  };

  if (loading) {
    return (
      <div className="flex h-64 items-center justify-center">
        <Spinner size="lg" />
      </div>
    );
  }

  if (!album) {
    return <EmptyState title="Album not found" description="This album may have been deleted" />;
  }

  return (
    <div>
      <div className="flex items-center gap-4 mb-6">
        <button
          onClick={() => router.back()}
          className="rounded-lg p-2 text-surface-500 hover:bg-surface-100"
        >
          <ArrowLeft className="h-5 w-5" />
        </button>
        <div className="flex-1">
          <h1 className="text-2xl font-bold text-surface-900">{album.name}</h1>
          {album.description && (
            <p className="text-sm text-surface-500">{album.description}</p>
          )}
        </div>
        <Button variant="danger" size="sm" onClick={() => setShowDelete(true)} className="gap-2">
          <Trash2 className="h-4 w-4" />
          Delete
        </Button>
      </div>

      {!album.media || album.media.length === 0 ? (
        <EmptyState
          title="No photos in this album"
          description="Add photos to this album to see them here"
        />
      ) : (
        <PhotoGrid items={album.media} onItemClick={handleItemClick} />
      )}

      {viewerIndex !== null && album.media && (
        <PhotoViewer
          items={album.media}
          currentIndex={viewerIndex}
          onClose={() => setViewerIndex(null)}
          onNavigate={setViewerIndex}
        />
      )}

      <ConfirmDialog
        open={showDelete}
        onClose={() => setShowDelete(false)}
        onConfirm={handleDelete}
        title="Delete album"
        description={`Are you sure you want to delete "${album.name}"? This cannot be undone.`}
        confirmLabel="Delete"
      />
    </div>
  );
}
