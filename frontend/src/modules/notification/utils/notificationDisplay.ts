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

export function getNotificationDescription(notification: NotificationItem, t: TFunction): string {
  return t(`notification.types.${notification.type}.description`, {
    actor: notification.actorFullName ?? t('notification.unknownActor'),
    group: notification.researchGroupName ?? t('notification.fallbackGroup'),
    project: notification.projectName ?? t('notification.fallbackProject'),
  });
}
