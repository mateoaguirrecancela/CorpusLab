export type ProjectParticipantRole = 'CREATOR' | 'PARTICIPANT';

export type ProjectParticipantAssignmentGroup = 'GROUP_A' | 'GROUP_B';

export type ProjectParticipantAssignment = {
  userId: number;
  iaaGroup: ProjectParticipantAssignmentGroup;
};

export type ProjectType =
  | 'TEXT_CLASSIFICATION_SIMPLE'
  | 'TEXT_CLASSIFICATION_MULTILABEL'
  | 'NER'
  | 'SEQ2SEQ';

export type ProjectMetricType =
  | 'COHENS_KAPPA'
  | 'KRIPPENDORFFS_ALPHA'
  | 'FLEISS_KAPPA'
  | 'XRR'
  | 'SPAN_OVERLAP_F1';

export type ProjectMetricStatus =
  | 'CALCULABLE'
  | 'NO_ANNOTATIONS'
  | 'INSUFFICIENT_ANNOTATORS'
  | 'INSUFFICIENT_ITEMS'
  | 'NO_SHARED_ITEMS'
  | 'NO_VALID_PAIRS'
  | 'UNDEFINED';

export type ProjectSetupLabel = {
  name: string;
  color: string | null;
};
