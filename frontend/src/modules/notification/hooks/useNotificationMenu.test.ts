import { renderHook } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { toast } from 'sonner';
import { useNotificationMenu } from '@/modules/notification/hooks/useNotificationMenu';
import {
  useMarkAllNotificationsAsReadMutation,
  useMarkNotificationAsReadMutation,
  useNotificationsQuery,
} from '@/modules/notification/hooks/useNotificationQueries';
import {
  getMarkAllNotificationsReadErrorMessage,
  getMarkNotificationReadErrorMessage,
  getNotificationsErrorMessage,
} from '@/modules/notification/services/notificationService';
import { type NotificationItem } from '@/modules/notification/types/notification';

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ i18n: {} }),
}));

vi.mock('@/app/config/i18n', () => ({
  getResolvedLanguage: () => 'en-US',
}));

vi.mock('sonner', () => ({
  toast: { error: vi.fn() },
}));

vi.mock('@/modules/notification/hooks/useNotificationQueries', () => ({
  useMarkAllNotificationsAsReadMutation: vi.fn(),
  useMarkNotificationAsReadMutation: vi.fn(),
  useNotificationsQuery: vi.fn(),
}));

vi.mock('@/modules/notification/services/notificationService', () => ({
  getMarkAllNotificationsReadErrorMessage: vi.fn(() => 'mark-all-error'),
  getMarkNotificationReadErrorMessage: vi.fn(() => 'mark-one-error'),
  getNotificationsErrorMessage: vi.fn(() => 'notifications-load-error'),
}));

const notification: NotificationItem = {
  id: 1,
  type: 'PROJECT_ARCHIVED',
  read: false,
  createdAt: '2026-01-01T00:00:00Z',
  actorFullName: null,
  researchGroupId: null,
  researchGroupName: null,
  invitationId: null,
  projectId: 1,
  projectName: 'Project',
  datasetItemId: null,
  datasetItemIndex: null,
  datasetItemName: null,
  annotationStepIndex: null,
};

const markOneMutate = vi.fn();
const markAllMutate = vi.fn();

beforeEach(() => {
  markOneMutate.mockClear();
  markAllMutate.mockClear();
  vi.mocked(toast.error).mockClear();
  vi.mocked(useMarkNotificationAsReadMutation).mockReturnValue({
    mutate: markOneMutate,
    isPending: false,
  } as never);
  vi.mocked(useMarkAllNotificationsAsReadMutation).mockReturnValue({
    mutate: markAllMutate,
    isPending: false,
  } as never);
  vi.mocked(useNotificationsQuery).mockReturnValue({
    data: { notifications: [notification], unreadCount: 1 },
    error: null,
    isError: false,
    isLoading: false,
  } as never);
});

describe('useNotificationMenu', () => {
  it('exposes the unread count and allows marking all as read when there are unread items', () => {
    const { result } = renderHook(() => useNotificationMenu());

    expect(result.current.unreadCount).toBe(1);
    expect(result.current.canMarkAllAsRead).toBe(true);
    expect(result.current.locale).toBe('en-US');
  });

  it('disables marking all as read while there is nothing unread', () => {
    vi.mocked(useNotificationsQuery).mockReturnValue({
      data: { notifications: [{ ...notification, read: true }], unreadCount: 0 },
      error: null,
      isError: false,
      isLoading: false,
    } as never);

    const { result } = renderHook(() => useNotificationMenu());

    expect(result.current.canMarkAllAsRead).toBe(false);
  });

  it('marks a single unread notification as read', () => {
    const { result } = renderHook(() => useNotificationMenu());

    result.current.markNotificationAsRead(notification);

    expect(markOneMutate).toHaveBeenCalledWith(1, expect.objectContaining({ onError: expect.any(Function) }));
  });

  it('ignores marking an already-read notification', () => {
    const { result } = renderHook(() => useNotificationMenu());

    result.current.markNotificationAsRead({ ...notification, read: true });

    expect(markOneMutate).not.toHaveBeenCalled();
  });

  it('ignores marking all as read when nothing is unread', () => {
    vi.mocked(useNotificationsQuery).mockReturnValue({
      data: { notifications: [{ ...notification, read: true }], unreadCount: 0 },
      error: null,
      isError: false,
      isLoading: false,
    } as never);
    const { result } = renderHook(() => useNotificationMenu());

    result.current.markAllNotificationsAsRead();

    expect(markAllMutate).not.toHaveBeenCalled();
  });

  it('shows an error toast when marking a single notification fails', () => {
    markOneMutate.mockImplementation((_id, { onError }) => onError(new Error('boom')));
    const { result } = renderHook(() => useNotificationMenu());

    result.current.markNotificationAsRead(notification);

    expect(getMarkNotificationReadErrorMessage).toHaveBeenCalled();
    expect(toast.error).toHaveBeenCalledWith('mark-one-error');
  });

  it('marks all as read and shows an error toast on failure', () => {
    markAllMutate.mockImplementation((_arg, { onError }) => onError(new Error('boom')));
    const { result } = renderHook(() => useNotificationMenu());

    result.current.markAllNotificationsAsRead();

    expect(markAllMutate).toHaveBeenCalled();
    expect(getMarkAllNotificationsReadErrorMessage).toHaveBeenCalled();
    expect(toast.error).toHaveBeenCalledWith('mark-all-error');
  });

  it('shows a load-error toast when the notifications query fails', () => {
    vi.mocked(useNotificationsQuery).mockReturnValue({
      data: undefined,
      error: new Error('network error'),
      isError: true,
      isLoading: false,
    } as never);

    renderHook(() => useNotificationMenu());

    expect(getNotificationsErrorMessage).toHaveBeenCalled();
    expect(toast.error).toHaveBeenCalledWith('notifications-load-error', {
      id: 'notifications-load-error',
    });
  });
});
