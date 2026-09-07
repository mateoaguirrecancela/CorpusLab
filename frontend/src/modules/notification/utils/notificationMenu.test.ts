import { describe, expect, it } from 'vitest';
import {
  canMarkAllNotificationsAsRead,
  canMarkNotificationAsRead,
  getNotificationListStatus,
  getNotifications,
  getUnreadCount,
} from '@/modules/notification/utils/notificationMenu';
import { type NotificationItem } from '@/modules/notification/types/notification';

const notification: NotificationItem = {
  id: 1,
  type: 'PROJECT_ARCHIVED',
  read: false,
  createdAt: '2026-01-01T00:00:00Z',
  actorFullName: 'Jane Doe',
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

describe('getNotifications / getUnreadCount', () => {
  it('return defaults when the response is undefined', () => {
    expect(getNotifications(undefined)).toEqual([]);
    expect(getUnreadCount(undefined)).toBe(0);
  });

  it('read fields from the response', () => {
    const response = { notifications: [notification], unreadCount: 3 };
    expect(getNotifications(response)).toEqual([notification]);
    expect(getUnreadCount(response)).toBe(3);
  });
});

describe('getNotificationListStatus', () => {
  it('prioritizes loading over error', () => {
    expect(getNotificationListStatus({ isLoading: true, isError: true, notifications: [] })).toBe(
      'loading',
    );
  });

  it('is error when not loading but errored', () => {
    expect(getNotificationListStatus({ isLoading: false, isError: true, notifications: [] })).toBe(
      'error',
    );
  });

  it('is empty when there are no notifications', () => {
    expect(getNotificationListStatus({ isLoading: false, isError: false, notifications: [] })).toBe(
      'empty',
    );
  });

  it('is ready when there are notifications', () => {
    expect(
      getNotificationListStatus({
        isLoading: false,
        isError: false,
        notifications: [notification],
      }),
    ).toBe('ready');
  });
});

describe('canMarkNotificationAsRead', () => {
  it('is true only when unread and not pending', () => {
    expect(canMarkNotificationAsRead(false, false)).toBe(true);
    expect(canMarkNotificationAsRead(true, false)).toBe(false);
    expect(canMarkNotificationAsRead(false, true)).toBe(false);
  });
});

describe('canMarkAllNotificationsAsRead', () => {
  it('is true only when there are unread items and not pending', () => {
    expect(canMarkAllNotificationsAsRead(1, false)).toBe(true);
    expect(canMarkAllNotificationsAsRead(0, false)).toBe(false);
    expect(canMarkAllNotificationsAsRead(1, true)).toBe(false);
  });
});
