import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { invalidateQueryKeys } from '@/lib/queryInvalidation';
import {
  getMyNotifications,
  markAllNotificationsAsRead,
  markNotificationAsRead,
} from '@/modules/notification/services/notificationService';

export const NOTIFICATIONS_QUERY_KEY = ['notifications'] as const;

export function useNotificationsQuery(limit = 12) {
  return useQuery({
    queryKey: [...NOTIFICATIONS_QUERY_KEY, limit],
    queryFn: () => getMyNotifications(limit),
    refetchInterval: 30_000,
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
