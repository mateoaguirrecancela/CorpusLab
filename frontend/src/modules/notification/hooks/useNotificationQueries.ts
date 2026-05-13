import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect, useMemo } from 'react';
import { invalidateQueryKeys } from '@/lib/queryInvalidation';
import {
  getMyNotifications,
  markAllNotificationsAsRead,
  markNotificationAsRead,
  subscribeToNotificationEvents,
} from '@/modules/notification/services/notificationService';

export const NOTIFICATIONS_QUERY_KEY = ['notifications'] as const;

export function useNotificationsQuery(limit = 12) {
  const queryClient = useQueryClient();
  const queryKey = useMemo(() => [...NOTIFICATIONS_QUERY_KEY, limit] as const, [limit]);

  useEffect(() => {
    return subscribeToNotificationEvents(() => {
      invalidateQueryKeys(queryClient, [queryKey]);
    });
  }, [queryClient, queryKey]);

  return useQuery({
    queryKey,
    queryFn: () => getMyNotifications(limit),
    refetchInterval: false,
  });
}

export function useMarkNotificationAsReadMutation(limit = 12) {
  const queryClient = useQueryClient();
  const queryKey = [...NOTIFICATIONS_QUERY_KEY, limit] as const;

  return useMutation({
    mutationFn: (notificationId: number) => markNotificationAsRead(notificationId),
    onSuccess: () => {
      invalidateQueryKeys(queryClient, [queryKey]);
    },
  });
}

export function useMarkAllNotificationsAsReadMutation(limit = 12) {
  const queryClient = useQueryClient();
  const queryKey = [...NOTIFICATIONS_QUERY_KEY, limit] as const;

  return useMutation({
    mutationFn: () => markAllNotificationsAsRead(),
    onSuccess: () => {
      invalidateQueryKeys(queryClient, [queryKey]);
    },
  });
}
