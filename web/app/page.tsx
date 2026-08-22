'use client';

import { useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { useAuth } from '@/lib/auth';
import { Spinner } from '@/components/ui/Spinner';

export default function HomePage() {
  const { user, loading } = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (!loading) {
      router.push(user ? '/photos' : '/login');
    }
  }, [loading, user, router]);

  return (
    <div className="flex h-screen items-center justify-center">
      <Spinner size="lg" />
    </div>
  );
}
