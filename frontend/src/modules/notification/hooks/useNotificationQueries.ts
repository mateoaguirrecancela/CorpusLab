import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect } from 'react';
import { invalidateNotificationChangeQueries } from '@/app/config/queryInvalidation';
import {
  getMyNotifications,
  markAllNotificationsAsRead,
  markNotificationAsRead,
  subscribeToNotificationEvents,
} from '@/modules/notification/services/notificationService';
import { notificationQueryKeys } from '@/modules/notification/queryKeys';

export function useNotificationsQuery(limit = 10) {
  const queryClient = useQueryClient();

  useEffect(() => {
    return subscribeToNotificationEvents(() => {
      invalidateNotificationChangeQueries(queryClient);
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
      invalidateNotificationChangeQueries(queryClient);
    },
  });
}

export function useMarkAllNotificationsAsReadMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: () => markAllNotificationsAsRead(),
    onSuccess: () => {
      invalidateNotificationChangeQueries(queryClient);
    },
  });
}
