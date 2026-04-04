import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
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

  return useMutation({
    mutationFn: (notificationId: number) => markNotificationAsRead(notificationId),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: [...NOTIFICATIONS_QUERY_KEY, limit] });
    },
  });
}

export function useMarkAllNotificationsAsReadMutation(limit = 12) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: () => markAllNotificationsAsRead(),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: [...NOTIFICATIONS_QUERY_KEY, limit] });
    },
  });
}
