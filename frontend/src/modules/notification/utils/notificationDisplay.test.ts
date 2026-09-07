import { describe, expect, it } from 'vitest';
import { type TFunction } from 'i18next';
import {
  formatNotificationDate,
  getNotificationDescription,
  getNotificationTitle,
} from '@/modules/notification/utils/notificationDisplay';
import { type NotificationItem } from '@/modules/notification/types/notification';

const t = ((key: string, opts?: Record<string, unknown>) =>
  opts ? `${key}:${JSON.stringify(opts)}` : key) as TFunction;

const baseNotification: NotificationItem = {
  id: 1,
  type: 'PROJECT_ANNOTATION_COMPLETED',
  read: false,
  createdAt: '2026-01-01T00:00:00Z',
  actorFullName: null,
  researchGroupId: null,
  researchGroupName: null,
  invitationId: null,
  projectId: 1,
  projectName: null,
  datasetItemId: null,
  datasetItemIndex: null,
  datasetItemName: null,
  annotationStepIndex: null,
};

describe('formatNotificationDate', () => {
  it('falls back to the raw value for an invalid date', () => {
    expect(formatNotificationDate('not-a-date', 'en')).toBe('not-a-date');
  });

  it('formats a valid date', () => {
    expect(formatNotificationDate('2026-01-01T00:00:00Z', 'en')).not.toBe('2026-01-01T00:00:00Z');
  });
});

describe('getNotificationTitle', () => {
  it('builds the translation key from the notification type', () => {
    expect(getNotificationTitle(baseNotification, t)).toBe(
      'notification.types.PROJECT_ANNOTATION_COMPLETED.title',
    );
  });
});

describe('getNotificationDescription', () => {
  it('falls back to default labels when data is missing', () => {
    const description = getNotificationDescription(baseNotification, t);
    const params = JSON.parse(description.split(':').slice(1).join(':')) as Record<string, unknown>;

    expect(params.actor).toBe('notification.unknownActor');
    expect(params.group).toBe('notification.fallbackGroup');
    expect(params.project).toBe('notification.fallbackProject');
    expect(params.item).toBe('notification.fallbackDatasetItem');
    expect(params.step).toBe('notification.fallbackAnnotationStep');
    expect(params.csvItemSuffix).toBe('');
  });

  it('uses the dataset item index fallback when there is no name', () => {
    const description = getNotificationDescription({ ...baseNotification, datasetItemIndex: 4 }, t);
    const params = JSON.parse(description.split(':').slice(1).join(':')) as Record<string, unknown>;

    expect(params.item).toBe('notification.fallbackDatasetItemWithIndex:{"index":5}');
  });

  it('appends the csv item suffix only for csv file names with a step index', () => {
    const description = getNotificationDescription(
      {
        ...baseNotification,
        datasetItemName: 'data.csv',
        annotationStepIndex: 2,
      },
      t,
    );
    const params = JSON.parse(description.split(':').slice(1).join(':')) as Record<string, unknown>;

    expect(params.csvItemSuffix).toBe('notification.csvItemSuffix:{"index":3}');
  });

  it('does not append the csv suffix for non-csv file names', () => {
    const description = getNotificationDescription(
      {
        ...baseNotification,
        datasetItemName: 'data.json',
        annotationStepIndex: 2,
      },
      t,
    );
    const params = JSON.parse(description.split(':').slice(1).join(':')) as Record<string, unknown>;

    expect(params.csvItemSuffix).toBe('');
  });
});
