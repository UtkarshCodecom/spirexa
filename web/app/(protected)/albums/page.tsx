'use client';

import { useState } from 'react';
import { useAlbumList } from '@/hooks/useAlbums';
import { AlbumCard } from '@/components/albums/AlbumCard';
import { CreateAlbumDialog } from '@/components/albums/CreateAlbumDialog';
import { EmptyState } from '@/components/photos/EmptyState';
import { Spinner } from '@/components/ui/Spinner';
import { Button } from '@/components/ui/Button';
import { Plus } from 'lucide-react';

export default function AlbumsPage() {
  const [showCreate, setShowCreate] = useState(false);
  const { data, loading, refetch } = useAlbumList();

  if (loading) {
    return (
      <div className="flex h-64 items-center justify-center">
        <Spinner size="lg" />
      </div>
    );
  }

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold text-surface-900">Albums</h1>
        <Button onClick={() => setShowCreate(true)} className="gap-2">
          <Plus className="h-4 w-4" />
          Create album
        </Button>
      </div>

      {data.length === 0 ? (
        <EmptyState
          icon="folder"
          title="No albums yet"
          description="Create your first album to organize your photos"
          actionLabel="Create album"
          onAction={() => setShowCreate(true)}
        />
      ) : (
        <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 gap-4">
          {data.map((album) => (
            <AlbumCard key={album.id} album={album} />
          ))}
        </div>
      )}

      <CreateAlbumDialog
        open={showCreate}
        onClose={() => setShowCreate(false)}
        onCreated={refetch}
      />
    </div>
  );
}
