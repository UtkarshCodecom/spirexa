'use client';

import { createContext, useContext, useState, useCallback, ReactNode } from 'react';
import { X, CheckCircle, AlertCircle, Info } from 'lucide-react';
import { cn } from '@/lib/utils';

interface Toast {
  id: string;
  type: 'success' | 'error' | 'info';
  message: string;
}

interface ToastContextType {
  addToast: (type: Toast['type'], message: string) => void;
}

const ToastContext = createContext<ToastContextType | undefined>(undefined);

export function ToastProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<Toast[]>([]);

  const addToast = useCallback((type: Toast['type'], message: string) => {
    const id = Math.random().toString(36).slice(2);
    setToasts((prev) => [...prev, { id, type, message }]);
    setTimeout(() => {
      setToasts((prev) => prev.filter((t) => t.id !== id));
    }, 5000);
  }, []);

  const removeToast = useCallback((id: string) => {
    setToasts((prev) => prev.filter((t) => t.id !== id));
  }, []);

  return (
    <ToastContext.Provider value={{ addToast }}>
      {children}
      <div className="fixed bottom-4 right-4 z-50 flex flex-col gap-2">
        {toasts.map((toast) => (
          <div
            key={toast.id}
            className={cn(
              'flex items-center gap-3 rounded-lg px-4 py-3 text-sm font-medium shadow-lg animate-in slide-in-from-right',
              {
                'bg-green-50 text-green-800 border border-green-200': toast.type === 'success',
                'bg-red-50 text-red-800 border border-red-200': toast.type === 'error',
                'bg-blue-50 text-blue-800 border border-blue-200': toast.type === 'info',
              }
            )}
          >
            {toast.type === 'success' && <CheckCircle className="h-4 w-4 text-green-600" />}
            {toast.type === 'error' && <AlertCircle className="h-4 w-4 text-red-600" />}
            {toast.type === 'info' && <Info className="h-4 w-4 text-blue-600" />}
            <span className="flex-1">{toast.message}</span>
            <button
              onClick={() => removeToast(toast.id)}
              className="rounded p-0.5 hover:bg-black/5"
            >
              <X className="h-3 w-3" />
            </button>
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  );
}

export function useToast() {
  const context = useContext(ToastContext);
  if (context === undefined) {
    throw new Error('useToast must be used within a ToastProvider');
  }
  return context;
}
