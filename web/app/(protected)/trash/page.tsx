'use client';

import { useState } from 'react';
import { useMediaList, useRestore, useDelete } from '@/hooks/useMedia';
import { PhotoGrid } from '@/components/photos/PhotoGrid';
import { PhotoViewer } from '@/components/photos/PhotoViewer';
import { EmptyState } from '@/components/photos/EmptyState';
import { ConfirmDialog } from '@/components/ui/ConfirmDialog';
import { Spinner } from '@/components/ui/Spinner';
import { Button } from '@/components/ui/Button';
import { Trash2 } from 'lucide-react';
import { useToast } from '@/components/ui/Toast';
import type { MediaItem } from '@/types';

export default function TrashPage() {
  const [viewerIndex, setViewerIndex] = useState<number | null>(null);
  const [showEmptyConfirm, setShowEmptyConfirm] = useState(false);
  const { data, loading, refetch } = useMediaList({ deleted: true, limit: 100 });
  const { restore } = useRestore();
  const { delete: permanentDelete } = useDelete();
  const { addToast } = useToast();

  const handleRestore = async (id: string) => {
    try {
      await restore(id);
      addToast('success', 'Photo restored');
      refetch();
    } catch {
      addToast('error', 'Failed to restore photo');
    }
  };

  const handlePermanentDelete = async (id: string) => {
    try {
      await permanentDelete(id);
      addToast('success', 'Photo permanently deleted');
      refetch();
    } catch {
      addToast('error', 'Failed to delete photo');
    }
  };

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
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-2xl font-bold text-surface-900">Trash</h1>
          <p className="text-sm text-surface-500">Items in trash are deleted after 30 days</p>
        </div>
        {data.length > 0 && (
          <Button
            variant="danger"
            size="sm"
            onClick={() => setShowEmptyConfirm(true)}
            className="gap-2"
          >
            <Trash2 className="h-4 w-4" />
            Empty trash
          </Button>
        )}
      </div>

      {data.length === 0 ? (
        <EmptyState
          icon="folder"
          title="Trash is empty"
          description="No items in trash"
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

      <ConfirmDialog
        open={showEmptyConfirm}
        onClose={() => setShowEmptyConfirm(false)}
        onConfirm={async () => {
          for (const item of data) {
            await permanentDelete(item.id);
          }
          addToast('success', 'Trash emptied');
          refetch();
          setShowEmptyConfirm(false);
        }}
        title="Empty trash"
        description="Permanently delete all items in trash? This cannot be undone."
        confirmLabel="Empty trash"
      />
    </div>
  );
}
