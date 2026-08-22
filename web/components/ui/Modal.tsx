'use client';

import { useEffect, useCallback, ReactNode } from 'react';
import { X } from 'lucide-react';
import { cn } from '@/lib/utils';

interface ModalProps {
  open: boolean;
  onClose: () => void;
  children: ReactNode;
  className?: string;
}

export function Modal({ open, onClose, children, className }: ModalProps) {
  const handleEscape = useCallback(
    (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose();
    },
    [onClose]
  );

  useEffect(() => {
    if (open) {
      document.addEventListener('keydown', handleEscape);
      document.body.style.overflow = 'hidden';
    }
    return () => {
      document.removeEventListener('keydown', handleEscape);
      document.body.style.overflow = '';
    };
  }, [open, handleEscape]);

  if (!open) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center">
      <div className="fixed inset-0 bg-black/50" onClick={onClose} />
      <div
        className={cn(
          'relative z-10 w-full max-w-lg rounded-xl bg-white p-6 shadow-xl',
          className
        )}
      >
        <button
          onClick={onClose}
          className="absolute right-4 top-4 rounded-lg p-1 text-surface-400 hover:bg-surface-100 hover:text-surface-600"
        >
          <X className="h-5 w-5" />
        </button>
        {children}
      </div>
    </div>
  );
}
