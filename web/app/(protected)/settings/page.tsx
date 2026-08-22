'use client';

import { useAuth } from '@/lib/auth';
import { Avatar } from '@/components/ui/Avatar';
import { Button } from '@/components/ui/Button';
import { useToast } from '@/components/ui/Toast';
import { User, HardDrive, LogOut } from 'lucide-react';

export default function SettingsPage() {
  const { user, signOut } = useAuth();
  const { addToast } = useToast();

  const handleSignOut = async () => {
    try {
      await signOut();
      addToast('success', 'Signed out');
    } catch {
      addToast('error', 'Failed to sign out');
    }
  };

  return (
    <div className="max-w-2xl">
      <h1 className="text-2xl font-bold text-surface-900 mb-6">Settings</h1>

      <div className="space-y-6">
        <section className="rounded-xl bg-white border border-surface-200 p-6">
          <h2 className="text-lg font-semibold text-surface-900 flex items-center gap-2 mb-4">
            <User className="h-5 w-5" />
            Profile
          </h2>
          <div className="flex items-center gap-4">
            <Avatar
              src={user?.photoURL}
              name={user?.displayName || user?.email || ''}
              size="lg"
            />
            <div>
              <p className="font-medium text-surface-900">{user?.displayName || 'User'}</p>
              <p className="text-sm text-surface-500">{user?.email}</p>
            </div>
          </div>
        </section>

        <section className="rounded-xl bg-white border border-surface-200 p-6">
          <h2 className="text-lg font-semibold text-surface-900 flex items-center gap-2 mb-4">
            <HardDrive className="h-5 w-5" />
            Storage
          </h2>
          <div className="space-y-3">
            <div className="flex justify-between text-sm">
              <span className="text-surface-600">Used</span>
              <span className="font-medium text-surface-900">0 MB / 15 GB</span>
            </div>
            <div className="h-2 rounded-full bg-surface-100 overflow-hidden">
              <div className="h-full w-0 rounded-full bg-primary-500" />
            </div>
          </div>
        </section>

        <section className="rounded-xl bg-white border border-surface-200 p-6">
          <h2 className="text-lg font-semibold text-surface-900 mb-4">Account</h2>
          <Button variant="danger" onClick={handleSignOut} className="gap-2">
            <LogOut className="h-4 w-4" />
            Sign out
          </Button>
        </section>
      </div>
    </div>
  );
}
