export type NotificationType =
  | 'RESEARCH_GROUP_INVITATION_RECEIVED'
  | 'RESEARCH_GROUP_INVITATION_ACCEPTED';

export type NotificationItem = {
  id: number;
  type: NotificationType;
  read: boolean;
  createdAt: string;
  actorFullName: string | null;
  researchGroupId: number | null;
  researchGroupName: string | null;
  invitationId: number | null;
};

export type NotificationListResponse = {
  notifications: NotificationItem[];
  unreadCount: number;
};
