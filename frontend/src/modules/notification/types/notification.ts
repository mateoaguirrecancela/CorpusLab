export type NotificationType =
  | 'RESEARCH_GROUP_INVITATION_RECEIVED'
  | 'RESEARCH_GROUP_INVITATION_ACCEPTED'
  | 'RESEARCH_GROUP_INVITATION_DECLINED'
  | 'RESEARCH_GROUP_MEMBER_JOINED_BY_CODE'
  | 'RESEARCH_GROUP_MEMBER_ROLE_UPDATED'
  | 'RESEARCH_GROUP_MEMBER_REMOVED'
  | 'PROJECT_PARTICIPANT_ASSIGNED'
  | 'PROJECT_PARTICIPANT_UNASSIGNED'
  | 'PROJECT_ARCHIVED'
  | 'PROJECT_DELETED'
  | 'PROJECT_ANNOTATION_COMPLETED'
  | 'ANNOTATION_WARNING_MARKED'
  | 'ANNOTATION_WARNING_CLEARED'
  | 'ANNOTATION_WARNING_RESOLVED';

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
  datasetItemId: number | null;
  datasetItemIndex: number | null;
  datasetItemName: string | null;
  annotationStepIndex: number | null;
};

export type NotificationListResponse = {
  notifications: NotificationItem[];
  unreadCount: number;
};
