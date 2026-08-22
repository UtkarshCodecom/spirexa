'use client';

import { useState, useRef } from 'react';
import { Upload } from 'lucide-react';
import { Button } from '@/components/ui/Button';
import { useToast } from '@/components/ui/Toast';
import { apiClient } from '@/lib/api';
import { auth } from '@/lib/firebase';
import { UploadProgress } from './UploadProgress';
import type { UploadProgress as UploadProgressType } from '@/types';

export function UploadButton() {
  const [uploads, setUploads] = useState<UploadProgressType[]>([]);
  const [showProgress, setShowProgress] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const { addToast } = useToast();

  const handleFiles = async (files: FileList) => {
    const newUploads: UploadProgressType[] = Array.from(files).map((file) => ({
      id: Math.random().toString(36).slice(2),
      file,
      progress: 0,
      status: 'pending' as const,
    }));

    setUploads((prev) => [...prev, ...newUploads]);
    setShowProgress(true);

    for (const upload of newUploads) {
      try {
        setUploads((prev) =>
          prev.map((u) => (u.id === upload.id ? { ...u, status: 'uploading' as const } : u))
        );

        if (!auth.currentUser) throw new Error('Not authenticated');

        // Streams straight through to the server, which streams it on to
        // storage — one call, no direct client-to-storage upload.
        await apiClient.upload(upload.file);

        setUploads((prev) =>
          prev.map((u) => (u.id === upload.id ? { ...u, status: 'completed' as const, progress: 100 } : u))
        );
      } catch {
        setUploads((prev) =>
          prev.map((u) =>
            u.id === upload.id ? { ...u, status: 'error' as const, error: 'Upload failed' } : u)
        );
        addToast('error', `Failed to upload ${upload.file.name}`);
      }
    }

    addToast('success', 'Uploads completed');
  };

  return (
    <>
      <input
        ref={fileInputRef}
        type="file"
        multiple
        accept="image/*,video/*"
        className="hidden"
        onChange={(e) => e.target.files && handleFiles(e.target.files)}
      />
      <Button onClick={() => fileInputRef.current?.click()} className="gap-2">
        <Upload className="h-4 w-4" />
        Upload
      </Button>

      {showProgress && (
        <UploadProgress
          uploads={uploads}
          onClose={() => {
            setShowProgress(false);
            setUploads([]);
          }}
        />
      )}
    </>
  );
}
