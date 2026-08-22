'use client';

import { useState, useRef, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { Menu, Search, Settings, LogOut, ChevronDown } from 'lucide-react';
import { useAuth } from '@/lib/auth';
import { Avatar } from '@/components/ui/Avatar';

interface TopBarProps {
  onMenuClick: () => void;
}

export function TopBar({ onMenuClick }: TopBarProps) {
  const [searchQuery, setSearchQuery] = useState('');
  const [dropdownOpen, setDropdownOpen] = useState(false);
  const { user, signOut } = useAuth();
  const router = useRouter();
  const dropdownRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    function handleClickOutside(e: MouseEvent) {
      if (dropdownRef.current && !dropdownRef.current.contains(e.target as Node)) {
        setDropdownOpen(false);
      }
    }
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    if (searchQuery.trim()) {
      router.push(`/search?q=${encodeURIComponent(searchQuery.trim())}`);
    }
  };

  return (
    <header className="sticky top-0 z-30 bg-white border-b border-surface-200">
      <div className="flex items-center gap-4 px-4 py-3">
        <button
          onClick={onMenuClick}
          className="rounded-lg p-2 text-surface-500 hover:bg-surface-100 lg:hidden"
        >
          <Menu className="h-5 w-5" />
        </button>

        <form onSubmit={handleSearch} className="flex-1 max-w-xl">
          <div className="relative">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-surface-400" />
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="Search photos..."
              className="w-full rounded-full bg-surface-100 py-2 pl-10 pr-4 text-sm placeholder:text-surface-400 focus:outline-none focus:ring-2 focus:ring-primary-500 focus:bg-white"
            />
          </div>
        </form>

        <div className="relative" ref={dropdownRef}>
          <button
            onClick={() => setDropdownOpen(!dropdownOpen)}
            className="flex items-center gap-2 rounded-lg p-1 hover:bg-surface-100"
          >
            <Avatar
              src={user?.photoURL}
              name={user?.displayName || user?.email || ''}
              size="sm"
            />
            <ChevronDown className="h-4 w-4 text-surface-500 hidden sm:block" />
          </button>

          {dropdownOpen && (
            <div className="absolute right-0 top-full mt-2 w-56 rounded-lg bg-white border border-surface-200 shadow-lg py-1">
              <div className="px-4 py-2 border-b border-surface-100">
                <p className="text-sm font-medium text-surface-900">{user?.displayName || 'User'}</p>
                <p className="text-xs text-surface-500">{user?.email}</p>
              </div>
              <button
                onClick={() => {
                  setDropdownOpen(false);
                  router.push('/settings');
                }}
                className="flex w-full items-center gap-2 px-4 py-2 text-sm text-surface-700 hover:bg-surface-50"
              >
                <Settings className="h-4 w-4" />
                Settings
              </button>
              <button
                onClick={() => {
                  setDropdownOpen(false);
                  signOut();
                }}
                className="flex w-full items-center gap-2 px-4 py-2 text-sm text-surface-700 hover:bg-surface-50"
              >
                <LogOut className="h-4 w-4" />
                Sign out
              </button>
            </div>
          )}
        </div>
      </div>
    </header>
  );
}
