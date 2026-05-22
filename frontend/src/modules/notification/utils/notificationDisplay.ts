import { type TFunction } from 'i18next';
import { formatDate } from '@/lib/dateFormatters';
import { type NotificationItem } from '@/modules/notification/types/notification';

export function formatNotificationDate(createdAt: string, locale: string): string {
  return formatDate(createdAt, locale, {
    dateStyle: 'short',
    fallback: createdAt,
    timeStyle: 'short',
  });
}

export function getNotificationTitle(notification: NotificationItem, t: TFunction): string {
  return t(`notification.types.${notification.type}.title`);
}

function getNotificationDatasetItemLabel(
  notification: NotificationItem,
  t: TFunction,
): string {
  if (notification.datasetItemName && notification.datasetItemName.trim().length > 0) {
    return notification.datasetItemName;
  }

  if (notification.datasetItemIndex != null) {
    return t('notification.fallbackDatasetItemWithIndex', {
      index: notification.datasetItemIndex + 1,
    });
  }

  return t('notification.fallbackDatasetItem');
}

function isCsvFileName(fileName: string): boolean {
  return fileName.trim().toLowerCase().endsWith('.csv');
}

function getNotificationDatasetFileLabel(
  notification: NotificationItem,
  t: TFunction,
): string {
  if (notification.datasetItemName && notification.datasetItemName.trim().length > 0) {
    return notification.datasetItemName.trim();
  }

  if (notification.datasetItemIndex != null) {
    return t('notification.fallbackDatasetFileWithIndex', {
      index: notification.datasetItemIndex + 1,
    });
  }

  return t('notification.fallbackDatasetFile');
}

function getNotificationCsvItemSuffix(
  notification: NotificationItem,
  datasetFileLabel: string,
  t: TFunction,
): string {
  if (notification.annotationStepIndex == null || !isCsvFileName(datasetFileLabel)) {
    return '';
  }

  return t('notification.csvItemSuffix', { index: notification.annotationStepIndex + 1 });
}

export function getNotificationDescription(notification: NotificationItem, t: TFunction): string {
  const datasetFileLabel = getNotificationDatasetFileLabel(notification, t);

  return t(`notification.types.${notification.type}.description`, {
    actor: notification.actorFullName ?? t('notification.unknownActor'),
    csvItemSuffix: getNotificationCsvItemSuffix(notification, datasetFileLabel, t),
    file: datasetFileLabel,
    group: notification.researchGroupName ?? t('notification.fallbackGroup'),
    item: getNotificationDatasetItemLabel(notification, t),
    project: notification.projectName ?? t('notification.fallbackProject'),
    step:
      notification.annotationStepIndex == null
        ? t('notification.fallbackAnnotationStep')
        : notification.annotationStepIndex + 1,
  });
}
