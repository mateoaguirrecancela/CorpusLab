import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect } from 'react';
import { invalidateQueryKeys } from '@/lib/queryInvalidation';
import {
  getMyNotifications,
  markAllNotificationsAsRead,
  markNotificationAsRead,
  subscribeToNotificationEvents,
} from '@/modules/notification/services/notificationService';

export const NOTIFICATIONS_QUERY_KEY = ['notifications'] as const;

export const notificationQueryKeys = {
  all: NOTIFICATIONS_QUERY_KEY,
  list: (limit: number) => [...NOTIFICATIONS_QUERY_KEY, limit] as const,
};

export function useNotificationsQuery(limit = 12) {
  const queryClient = useQueryClient();

  useEffect(() => {
    return subscribeToNotificationEvents(() => {
      invalidateQueryKeys(queryClient, [notificationQueryKeys.all]);
    });
  }, [queryClient]);

  return useQuery({
    queryKey: notificationQueryKeys.list(limit),
    queryFn: () => getMyNotifications(limit),
    refetchInterval: false,
  });
}

export function useMarkNotificationAsReadMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (notificationId: number) => markNotificationAsRead(notificationId),
    onSuccess: () => {
      invalidateQueryKeys(queryClient, [notificationQueryKeys.all]);
    },
  });
}

export function useMarkAllNotificationsAsReadMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: () => markAllNotificationsAsRead(),
    onSuccess: () => {
      invalidateQueryKeys(queryClient, [notificationQueryKeys.all]);
    },
  });
}
