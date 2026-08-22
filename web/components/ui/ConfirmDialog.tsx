'use client';

import { Modal } from './Modal';
import { Button } from './Button';

interface ConfirmDialogProps {
  open: boolean;
  onClose: () => void;
  onConfirm: () => void;
  title: string;
  description: string;
  confirmLabel?: string;
  loading?: boolean;
}

export function ConfirmDialog({
  open,
  onClose,
  onConfirm,
  title,
  description,
  confirmLabel = 'Confirm',
  loading,
}: ConfirmDialogProps) {
  return (
    <Modal open={open} onClose={onClose} className="max-w-sm">
      <div className="text-center">
        <h3 className="text-lg font-semibold text-surface-900">{title}</h3>
        <p className="mt-2 text-sm text-surface-500">{description}</p>
      </div>
      <div className="mt-6 flex gap-3 justify-end">
        <Button variant="secondary" onClick={onClose} disabled={loading}>
          Cancel
        </Button>
        <Button variant="danger" onClick={onConfirm} disabled={loading}>
          {loading ? 'Processing...' : confirmLabel}
        </Button>
      </div>
    </Modal>
  );
}
