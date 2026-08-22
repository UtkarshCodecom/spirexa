'use client';

import { useState } from 'react';
import { Modal } from '@/components/ui/Modal';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { useCreateAlbum } from '@/hooks/useAlbums';
import { useToast } from '@/components/ui/Toast';

interface CreateAlbumDialogProps {
  open: boolean;
  onClose: () => void;
  onCreated: () => void;
}

export function CreateAlbumDialog({ open, onClose, onCreated }: CreateAlbumDialogProps) {
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const { create, loading } = useCreateAlbum();
  const { addToast } = useToast();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim()) return;

    try {
      await create(name.trim(), description.trim() || undefined);
      addToast('success', 'Album created');
      setName('');
      setDescription('');
      onCreated();
      onClose();
    } catch {
      addToast('error', 'Failed to create album');
    }
  };

  return (
    <Modal open={open} onClose={onClose}>
      <h2 className="text-lg font-semibold text-surface-900">Create album</h2>
      <form onSubmit={handleSubmit} className="mt-4 space-y-4">
        <Input
          label="Name"
          value={name}
          onChange={(e) => setName(e.target.value)}
          placeholder="Album name"
          required
        />
        <div>
          <label className="block text-sm font-medium text-surface-700 mb-1">Description</label>
          <textarea
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            placeholder="Optional description"
            rows={3}
            className="flex w-full rounded-lg border border-surface-300 bg-white px-3 py-2 text-sm placeholder:text-surface-400 focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent"
          />
        </div>
        <div className="flex justify-end gap-3">
          <Button type="button" variant="secondary" onClick={onClose} disabled={loading}>
            Cancel
          </Button>
          <Button type="submit" disabled={loading || !name.trim()}>
            {loading ? 'Creating...' : 'Create'}
          </Button>
        </div>
      </form>
    </Modal>
  );
}
