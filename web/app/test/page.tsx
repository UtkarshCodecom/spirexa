'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { useAuth } from '@/lib/auth';
import { Button } from '@/components/ui/Button';
import { Images, Search, MapPin, FolderOpen, ShieldCheck } from 'lucide-react';

// Demo account for hackathon judges — a real Firebase user seeded with a
// sample photo library, not a real user's account. Intentionally public: the
// whole point is a one-click way in for reviewers, no credentials to copy.
const DEMO_EMAIL = 'test@gmail.com';
const DEMO_PASSWORD = 'Utkarsh.1905';

export default function TestPage() {
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const { signIn, user } = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (user) router.push('/photos');
  }, [user, router]);

  if (user) return null;

  const handleDemoLogin = async () => {
    setError('');
    setLoading(true);
    try {
      await signIn(DEMO_EMAIL, DEMO_PASSWORD);
      router.push('/photos');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Could not sign in to the demo account');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-surface-50 px-4">
      <div className="w-full max-w-md">
        <div className="mb-8 text-center">
          <div className="mx-auto mb-4 flex h-14 w-14 items-center justify-center rounded-2xl bg-primary-600">
            <Images className="h-8 w-8 text-white" />
          </div>
          <h1 className="text-2xl font-bold text-surface-900">Judge preview</h1>
          <p className="mt-1 text-sm text-surface-500">
            No account needed — one click into a real, pre-populated library.
          </p>
        </div>

        <div className="rounded-xl bg-white p-6 shadow-sm border border-surface-200">
          <Button onClick={handleDemoLogin} className="w-full" disabled={loading}>
            {loading ? 'Signing in…' : 'View demo library'}
          </Button>
          {error && <p className="mt-3 text-sm text-red-600">{error}</p>}

          <div className="mt-6 space-y-3 border-t border-surface-100 pt-6 text-sm text-surface-600">
            <p className="font-medium text-surface-900">What to look at:</p>
            <div className="flex items-start gap-3">
              <Search className="mt-0.5 h-4 w-4 shrink-0 text-primary-600" />
              <span>
                <span className="font-medium text-surface-900">Search</span> — try “receipt” or
                any everyday word. Matches an AI-generated caption for each photo, not the
                filename.
              </span>
            </div>
            <div className="flex items-start gap-3">
              <ShieldCheck className="mt-0.5 h-4 w-4 shrink-0 text-primary-600" />
              <span>
                <span className="font-medium text-surface-900">Documents filter</span> (inside
                Search) — photos of receipts/IDs the AI auto-detected and transcribed.
              </span>
            </div>
            <div className="flex items-start gap-3">
              <FolderOpen className="mt-0.5 h-4 w-4 shrink-0 text-primary-600" />
              <span>
                <span className="font-medium text-surface-900">Albums</span> — open any photo →
                info panel shows the AI caption, tags, and transcribed text.
              </span>
            </div>
            <div className="flex items-start gap-3">
              <MapPin className="mt-0.5 h-4 w-4 shrink-0 text-primary-600" />
              <span>
                <span className="font-medium text-surface-900">Places</span> — photos clustered
                by where they were taken, from EXIF GPS.
              </span>
            </div>
          </div>
        </div>

        <p className="mt-6 text-center text-xs text-surface-400">
          This is a seeded demo account for review purposes — see the repository&apos;s README
          for architecture, security, and setup details.
        </p>
      </div>
    </div>
  );
}
