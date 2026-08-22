'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { useAuth } from '@/lib/auth';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Images } from 'lucide-react';

export default function LoginPage() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const { signIn, signInWithGoogle, user } = useAuth();
  const router = useRouter();

  // Redirecting must happen in an effect, not during render — calling
  // router.push() directly in the component body updates the Router
  // component while LoginPage is still rendering, which React rejects.
  useEffect(() => {
    if (user) router.push('/photos');
  }, [user, router]);

  if (user) {
    return null;
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      await signIn(email, password);
      router.push('/photos');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to sign in');
    } finally {
      setLoading(false);
    }
  };

  const handleGoogleSignIn = async () => {
    setError('');
    try {
      await signInWithGoogle();
      router.push('/photos');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to sign in with Google');
    }
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-surface-50 px-4">
      <div className="w-full max-w-sm">
        <div className="mb-8 text-center">
          <div className="mx-auto mb-4 flex h-14 w-14 items-center justify-center rounded-2xl bg-primary-600">
            <Images className="h-8 w-8 text-white" />
          </div>
          <h1 className="text-2xl font-bold text-surface-900">Welcome back</h1>
          <p className="mt-1 text-sm text-surface-500">Sign in to your account</p>
        </div>

        <div className="rounded-xl bg-white p-6 shadow-sm border border-surface-200">
          <form onSubmit={handleSubmit} className="space-y-4">
            <Input
              label="Email"
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="you@example.com"
              required
            />
            <Input
              label="Password"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="••••••••"
              required
            />

            {error && (
              <p className="text-sm text-red-600">{error}</p>
            )}

            <Button type="submit" className="w-full" disabled={loading}>
              {loading ? 'Signing in...' : 'Sign in'}
            </Button>
          </form>

          <div className="my-4 flex items-center gap-3">
            <div className="h-px flex-1 bg-surface-200" />
            <span className="text-xs text-surface-400">or</span>
            <div className="h-px flex-1 bg-surface-200" />
          </div>

          <Button
            variant="secondary"
            className="w-full"
            onClick={handleGoogleSignIn}
          >
            Sign in with Google
          </Button>
        </div>

        <p className="mt-6 text-center text-sm text-surface-500">
          Don&apos;t have an account?{' '}
          <Link href="/signup" className="font-medium text-primary-600 hover:text-primary-700">
            Sign up
          </Link>
        </p>
      </div>
    </div>
  );
}
