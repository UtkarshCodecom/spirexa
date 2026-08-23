import { clsx, type ClassValue } from 'clsx';
import { twMerge } from 'tailwind-merge';
import type { MediaItem } from '@/types';

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

export function formatDate(date: Date | string): string {
  const d = new Date(date);
  return d.toLocaleDateString('en-US', { month: 'long', year: 'numeric' });
}

export function formatFileSize(bytes: number): string {
  if (bytes === 0) return '0 B';
  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return `${parseFloat((bytes / Math.pow(k, i)).toFixed(1))} ${sizes[i]}`;
}

interface DateGroup {
  label: string;
  items: MediaItem[];
}

export function groupMediaByDate(items: MediaItem[]): DateGroup[] {
  const groups: Record<string, MediaItem[]> = {};

  for (const item of items) {
    // takenAt (EXIF capture date, falling back to upload time only when
    // unknown) — not createdAt, which is just when the file reached the
    // server and says nothing about when the photo was actually taken.
    const date = new Date(item.takenAt ?? item.createdAt);
    const label = date.toLocaleDateString('en-US', { month: 'long', year: 'numeric' });
    if (!groups[label]) {
      groups[label] = [];
    }
    groups[label].push(item);
  }

  return Object.entries(groups).map(([label, items]) => ({ label, items }));
}
