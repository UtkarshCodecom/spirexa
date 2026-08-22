'use client';

import { FolderOpen, Image as ImageIcon } from 'lucide-react';
import { Button } from '@/components/ui/Button';

interface EmptyStateProps {
  icon?: 'folder' | 'image';
  title: string;
  description: string;
  actionLabel?: string;
  onAction?: () => void;
}

export function EmptyState({
  icon = 'image',
  title,
  description,
  actionLabel,
  onAction,
}: EmptyStateProps) {
  return (
    <div className="flex flex-col items-center justify-center py-20 text-center">
      <div className="mb-4 rounded-full bg-surface-100 p-4">
        {icon === 'folder' ? (
          <FolderOpen className="h-10 w-10 text-surface-400" />
        ) : (
          <ImageIcon className="h-10 w-10 text-surface-400" />
        )}
      </div>
      <h3 className="text-lg font-semibold text-surface-900">{title}</h3>
      <p className="mt-1 max-w-sm text-sm text-surface-500">{description}</p>
      {actionLabel && onAction && (
        <Button onClick={onAction} className="mt-6">
          {actionLabel}
        </Button>
      )}
    </div>
  );
}
