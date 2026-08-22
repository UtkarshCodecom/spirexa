'use client';

import { EmptyState } from '@/components/photos/EmptyState';

export default function SharedPage() {
  return (
    <div>
      <h1 className="text-2xl font-bold text-surface-900 mb-6">Shared</h1>
      <EmptyState
        icon="folder"
        title="No shared photos"
        description="Photos shared with you will appear here"
      />
    </div>
  );
}
