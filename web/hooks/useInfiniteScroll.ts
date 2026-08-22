'use client';

import { useEffect, useRef, useCallback } from 'react';

export function useInfiniteScroll(
  fetchMore: () => void,
  hasMore: boolean,
  loading: boolean
) {
  const sentinelRef = useRef<HTMLDivElement | null>(null);

  const handleIntersect = useCallback(
    (entries: IntersectionObserverEntry[]) => {
      const [entry] = entries;
      if (entry.isIntersecting && hasMore && !loading) {
        fetchMore();
      }
    },
    [fetchMore, hasMore, loading]
  );

  useEffect(() => {
    const sentinel = sentinelRef.current;
    if (!sentinel) return;

    const observer = new IntersectionObserver(handleIntersect, {
      rootMargin: '200px',
    });

    observer.observe(sentinel);

    return () => {
      observer.disconnect();
    };
  }, [handleIntersect]);

  return sentinelRef;
}
