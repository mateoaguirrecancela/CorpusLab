import { renderHook } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { toast } from 'sonner';
import { useToastMessages } from '@/shared/hooks/useToastMessages';

vi.mock('sonner', () => ({
  toast: { error: vi.fn(), success: vi.fn() },
}));

beforeEach(() => {
  vi.mocked(toast.error).mockClear();
  vi.mocked(toast.success).mockClear();
});

describe('useToastMessages', () => {
  it('shows an error toast with an id when one is given', () => {
    renderHook(() => useToastMessages({ errorMessage: 'boom', errorToastId: 'my-id' }));

    expect(toast.error).toHaveBeenCalledWith('boom', { id: 'my-id' });
  });

  it('shows an error toast without an id when none is given', () => {
    renderHook(() => useToastMessages({ errorMessage: 'boom' }));

    expect(toast.error).toHaveBeenCalledWith('boom');
  });

  it('does not show an error toast for a blank or missing message', () => {
    renderHook(() => useToastMessages({ errorMessage: '   ' }));
    renderHook(() => useToastMessages({}));

    expect(toast.error).not.toHaveBeenCalled();
  });

  it('shows a success toast when a success message is given', () => {
    renderHook(() => useToastMessages({ successMessage: 'saved' }));

    expect(toast.success).toHaveBeenCalledWith('saved');
  });

  it('does not show a success toast for a blank or missing message', () => {
    renderHook(() => useToastMessages({ successMessage: '  ' }));
    renderHook(() => useToastMessages({}));

    expect(toast.success).not.toHaveBeenCalled();
  });
});
