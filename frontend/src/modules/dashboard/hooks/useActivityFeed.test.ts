import { renderHook } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { useActivityFeed } from '@/modules/dashboard/hooks/useActivityFeed';
import {
  useMarkNotificationAsReadMutation,
  useNotificationsQuery,
} from '@/modules/notification/hooks/useNotificationQueries';
import { type NotificationItem } from '@/modules/notification/types/notification';

const mutate = vi.fn();

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ i18n: {} }),
}));

vi.mock('@/app/config/i18n', () => ({
  getResolvedLanguage: () => 'en-US',
}));

vi.mock('@/modules/notification/hooks/useNotificationQueries', () => ({
  useMarkNotificationAsReadMutation: vi.fn(),
  useNotificationsQuery: vi.fn(),
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

beforeEach(() => {
  mutate.mockClear();
  vi.mocked(useMarkNotificationAsReadMutation).mockReturnValue({ mutate } as never);
});

describe('useActivityFeed', () => {
  it('reports the loading status while the query is pending', () => {
    vi.mocked(useNotificationsQuery).mockReturnValue({
      data: undefined,
      isLoading: true,
      isError: false,
    } as never);

    const { result } = renderHook(() => useActivityFeed());

    expect(result.current.listStatus).toBe('loading');
    expect(result.current.notifications).toEqual([]);
  });

  it('exposes the resolved locale and the loaded notifications', () => {
    vi.mocked(useNotificationsQuery).mockReturnValue({
      data: { notifications: [notification], unreadCount: 1 },
      isLoading: false,
      isError: false,
    } as never);

    const { result } = renderHook(() => useActivityFeed());

    expect(result.current.listStatus).toBe('ready');
    expect(result.current.notifications).toEqual([notification]);
    expect(result.current.locale).toBe('en-US');
  });

  it('marks an unread notification as read', () => {
    vi.mocked(useNotificationsQuery).mockReturnValue({
      data: { notifications: [notification], unreadCount: 1 },
      isLoading: false,
      isError: false,
    } as never);
    const { result } = renderHook(() => useActivityFeed());

    result.current.markNotificationAsRead(notification);

    expect(mutate).toHaveBeenCalledWith(1);
  });

  it('does nothing when the notification is already read', () => {
    const readNotification = { ...notification, read: true };
    vi.mocked(useNotificationsQuery).mockReturnValue({
      data: { notifications: [readNotification], unreadCount: 0 },
      isLoading: false,
      isError: false,
    } as never);
    const { result } = renderHook(() => useActivityFeed());

    result.current.markNotificationAsRead(readNotification);

    expect(mutate).not.toHaveBeenCalled();
  });
});
