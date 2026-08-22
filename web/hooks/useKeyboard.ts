'use client';

import { useEffect, useCallback } from 'react';

interface UseKeyboardOptions {
  onLeft?: () => void;
  onRight?: () => void;
  onEscape?: () => void;
  enabled?: boolean;
}

export function useKeyboard({ onLeft, onRight, onEscape, enabled = true }: UseKeyboardOptions) {
  const handleKeyDown = useCallback(
    (e: KeyboardEvent) => {
      if (!enabled) return;

      switch (e.key) {
        case 'ArrowLeft':
          onLeft?.();
          break;
        case 'ArrowRight':
          onRight?.();
          break;
        case 'Escape':
          onEscape?.();
          break;
      }
    },
    [enabled, onLeft, onRight, onEscape]
  );

  useEffect(() => {
    if (!enabled) return;
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [enabled, handleKeyDown]);
}
