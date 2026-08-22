'use client';

import { X, CheckCircle, AlertCircle } from 'lucide-react';
import type { UploadProgress as UploadProgressType } from '@/types';

interface UploadProgressProps {
  uploads: UploadProgressType[];
  onClose: () => void;
}

export function UploadProgress({ uploads, onClose }: UploadProgressProps) {
  return (
    <div className="fixed bottom-4 right-4 z-50 w-80 rounded-xl bg-white border border-surface-200 shadow-xl">
      <div className="flex items-center justify-between px-4 py-3 border-b border-surface-100">
        <h3 className="text-sm font-medium text-surface-900">
          Uploading {uploads.length} file{uploads.length !== 1 ? 's' : ''}
        </h3>
        <button onClick={onClose} className="rounded p-1 text-surface-400 hover:bg-surface-100">
          <X className="h-4 w-4" />
        </button>
      </div>
      <div className="max-h-60 overflow-y-auto divide-y divide-surface-100">
        {uploads.map((upload) => (
          <div key={upload.id} className="px-4 py-3">
            <div className="flex items-center gap-3">
              <div className="flex-1 min-w-0">
                <p className="text-sm text-surface-900 truncate">{upload.file.name}</p>
                <div className="mt-1.5 h-1.5 rounded-full bg-surface-100 overflow-hidden">
                  <div
                    className={`h-full rounded-full transition-all ${
                      upload.status === 'error'
                        ? 'bg-red-500'
                        : upload.status === 'completed'
                        ? 'bg-green-500'
                        : 'bg-primary-500'
                    }`}
                    style={{ width: `${upload.progress}%` }}
                  />
                </div>
              </div>
              {upload.status === 'completed' && (
                <CheckCircle className="h-4 w-4 text-green-500 shrink-0" />
              )}
              {upload.status === 'error' && (
                <AlertCircle className="h-4 w-4 text-red-500 shrink-0" />
              )}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
