export type NotificationType =
  | 'RESEARCH_GROUP_INVITATION_RECEIVED'
  | 'RESEARCH_GROUP_INVITATION_ACCEPTED'
  | 'PROJECT_PARTICIPANT_ASSIGNED'
  | 'PROJECT_ANNOTATION_COMPLETED';

export type NotificationItem = {
  id: number;
  type: NotificationType;
  read: boolean;
  createdAt: string;
  actorFullName: string | null;
  researchGroupId: number | null;
  researchGroupName: string | null;
  invitationId: number | null;
  projectId: number | null;
  projectName: string | null;
};

export type NotificationListResponse = {
  notifications: NotificationItem[];
  unreadCount: number;
};
