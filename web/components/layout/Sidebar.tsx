'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { Images, FolderHeart, Share2, Trash2, Search, X } from 'lucide-react';
import { cn } from '@/lib/utils';
import { useAuth } from '@/lib/auth';
import { Avatar } from '@/components/ui/Avatar';

interface SidebarProps {
  open: boolean;
  onClose: () => void;
}

const navItems = [
  { label: 'Photos', href: '/photos', icon: Images },
  { label: 'Albums', href: '/albums', icon: FolderHeart },
  { label: 'Favorites', href: '/favorites', icon: Images },
  { label: 'Shared', href: '/shared', icon: Share2 },
  { label: 'Trash', href: '/trash', icon: Trash2 },
  { label: 'Search', href: '/search', icon: Search },
];

export function Sidebar({ open, onClose }: SidebarProps) {
  const pathname = usePathname();
  const { user } = useAuth();

  return (
    <>
      {open && (
        <div
          className="fixed inset-0 z-40 bg-black/50 lg:hidden"
          onClick={onClose}
        />
      )}
      <aside
        className={cn(
          'fixed top-0 left-0 z-40 h-full w-64 bg-white border-r border-surface-200 transform transition-transform duration-200 ease-in-out lg:translate-x-0 lg:static lg:z-auto',
          open ? 'translate-x-0' : '-translate-x-full'
        )}
      >
        <div className="flex items-center justify-between px-6 py-4 border-b border-surface-100">
          <Link href="/photos" className="flex items-center gap-2">
            <div className="h-8 w-8 rounded-lg bg-primary-600 flex items-center justify-center">
              <Images className="h-5 w-5 text-white" />
            </div>
            <span className="text-lg font-bold text-surface-900">Photos</span>
          </Link>
          <button
            onClick={onClose}
            className="rounded-lg p-1 text-surface-400 hover:bg-surface-100 lg:hidden"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        <nav className="px-3 py-4 space-y-1">
          {navItems.map((item) => {
            const Icon = item.icon;
            const isActive = pathname === item.href;
            return (
              <Link
                key={item.href}
                href={item.href}
                onClick={onClose}
                className={cn(
                  'flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition-colors',
                  isActive
                    ? 'bg-primary-50 text-primary-700'
                    : 'text-surface-600 hover:bg-surface-50 hover:text-surface-900'
                )}
              >
                <Icon className="h-5 w-5" />
                {item.label}
              </Link>
            );
          })}
        </nav>

        <div className="absolute bottom-0 left-0 right-0 p-4 border-t border-surface-100">
          <div className="flex items-center gap-3">
            <Avatar
              src={user?.photoURL}
              name={user?.displayName || user?.email || ''}
              size="sm"
            />
            <div className="flex-1 min-w-0">
              <p className="text-sm font-medium text-surface-900 truncate">
                {user?.displayName || 'User'}
              </p>
              <p className="text-xs text-surface-500 truncate">{user?.email}</p>
            </div>
          </div>
        </div>
      </aside>
    </>
  );
}
