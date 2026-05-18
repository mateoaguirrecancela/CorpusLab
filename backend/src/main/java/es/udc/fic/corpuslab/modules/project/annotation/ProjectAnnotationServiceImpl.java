package es.udc.fic.corpuslab.modules.project.annotation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;

import es.udc.fic.corpuslab.modules.auth.api.AuthApiService;
import es.udc.fic.corpuslab.modules.auth.api.dtos.UserInfo;
import es.udc.fic.corpuslab.modules.notification.services.NotificationService;
import es.udc.fic.corpuslab.modules.project.metrics.ProjectMetricsCacheService;
import es.udc.fic.corpuslab.modules.project.progress.ProjectProgressCalculator;
import es.udc.fic.corpuslab.modules.project.progress.ProjectProgressSnapshot;
import es.udc.fic.corpuslab.modules.project.annotation.dtos.ProjectAnnotationStepDto;
import es.udc.fic.corpuslab.modules.project.annotation.dtos.ProjectAnnotationWorkspaceDto;
import es.udc.fic.corpuslab.modules.project.setup.dtos.ProjectSetupLabelDto;
import es.udc.fic.corpuslab.modules.project.annotation.dtos.SaveProjectAnnotationStepRequestDto;
import es.udc.fic.corpuslab.modules.project.annotation.dtos.SaveProjectAnnotationStepResponseDto;
import es.udc.fic.corpuslab.modules.project.shared.entities.Annotation;
import es.udc.fic.corpuslab.modules.project.shared.entities.DatasetItem;
import es.udc.fic.corpuslab.modules.project.shared.entities.Project;
import es.udc.fic.corpuslab.modules.project.shared.entities.ProjectParticipant;
import es.udc.fic.corpuslab.modules.project.shared.enums.ProjectParticipantRole;
import es.udc.fic.corpuslab.modules.project.shared.exceptions.InvalidProjectDatasetException;
import es.udc.fic.corpuslab.modules.project.shared.exceptions.InvalidProjectParticipantsException;
import es.udc.fic.corpuslab.modules.project.shared.exceptions.ProjectNotFoundException;
import es.udc.fic.corpuslab.modules.project.shared.references.ProjectEntityReferenceService;
import es.udc.fic.corpuslab.modules.project.shared.repositories.AnnotationRepository;
import es.udc.fic.corpuslab.modules.project.shared.repositories.DatasetItemRepository;
import es.udc.fic.corpuslab.modules.project.shared.repositories.ProjectParticipantRepository;
import es.udc.fic.corpuslab.modules.project.shared.utils.ProjectAnnotationUtils;
import es.udc.fic.corpuslab.modules.project.shared.utils.ProjectConstants;
import es.udc.fic.corpuslab.modules.project.shared.utils.ProjectDatasetUtils;

@Service
public class ProjectAnnotationServiceImpl implements ProjectAnnotationService {

    private final ProjectParticipantRepository projectParticipantRepository;
    private final DatasetItemRepository datasetItemRepository;
    private final AnnotationRepository annotationRepository;
    private final AuthApiService authApiService;
    private final NotificationService notificationService;
    private final ProjectMetricsCacheService projectMetricsCacheService;
    private final ProjectEntityReferenceService entityReferenceService;
    private final ProjectProgressCalculator projectProgressCalculator;
    private final AnnotationPayloadNormalizer annotationPayloadNormalizer;
    private final AnnotationStepResolver annotationStepResolver;
    private final AnnotationWarningPolicy annotationWarningPolicy;

    @Autowired
    public ProjectAnnotationServiceImpl(
            ProjectParticipantRepository projectParticipantRepository,
            DatasetItemRepository datasetItemRepository,
            AnnotationRepository annotationRepository,
            AuthApiService authApiService,
            NotificationService notificationService,
            ProjectMetricsCacheService projectMetricsCacheService,
            ProjectEntityReferenceService entityReferenceService,
            ProjectProgressCalculator projectProgressCalculator,
            AnnotationPayloadNormalizer annotationPayloadNormalizer,
            AnnotationStepResolver annotationStepResolver,
            AnnotationWarningPolicy annotationWarningPolicy) {
        this.projectParticipantRepository = projectParticipantRepository;
        this.datasetItemRepository = datasetItemRepository;
        this.annotationRepository = annotationRepository;
        this.authApiService = authApiService;
        this.notificationService = notificationService;
        this.projectMetricsCacheService = projectMetricsCacheService;
        this.entityReferenceService = entityReferenceService;
        this.projectProgressCalculator = projectProgressCalculator;
        this.annotationPayloadNormalizer = annotationPayloadNormalizer;
        this.annotationStepResolver = annotationStepResolver;
        this.annotationWarningPolicy = annotationWarningPolicy;
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectAnnotationWorkspaceDto getAnnotationWorkspace(String authenticatedEmail, Long projectId, int offset,
            int limit) {
        UserInfo userInfo = authApiService.findUserByEmail(authenticatedEmail);

        ProjectParticipant participant = projectParticipantRepository
                .findByProjectIdAndUserId(projectId, userInfo.userId())
                .orElseThrow(() -> new ProjectNotFoundException(projectId));

        return buildAnnotationWorkspace(participant.getProject(), userInfo.userId(), offset, limit);
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectAnnotationWorkspaceDto getParticipantAnnotationWorkspaceForCreator(String authenticatedEmail,
            Long projectId, Long participantUserId, int offset, int limit) {
        UserInfo requesterInfo = authApiService.findUserByEmail(authenticatedEmail);

        ProjectParticipant requesterParticipant = projectParticipantRepository
                .findByProjectIdAndUserId(projectId, requesterInfo.userId())
                .orElseThrow(() -> new ProjectNotFoundException(projectId));

        if (requesterParticipant.getRole() != ProjectParticipantRole.CREATOR) {
            throw new AccessDeniedException("Only project creators can view investigators annotations");
        }

        ProjectParticipant targetParticipant = projectParticipantRepository
                .findByProjectIdAndUserId(projectId, participantUserId)
                .orElseThrow(() -> new InvalidProjectParticipantsException(
                        "Selected investigator is not assigned to this project"));

        if (targetParticipant.getRole() != ProjectParticipantRole.PARTICIPANT) {
            throw new InvalidProjectParticipantsException("Annotations can only be viewed for investigators");
        }

        return buildAnnotationWorkspace(requesterParticipant.getProject(), targetParticipant.getUser().getId(), offset,
                limit);
    }

    @Override
    @Transactional
    public SaveProjectAnnotationStepResponseDto saveAnnotationStep(String authenticatedEmail, Long projectId,
            SaveProjectAnnotationStepRequestDto request) {
        UserInfo userInfo = authApiService.findUserByEmail(authenticatedEmail);

        ProjectParticipant participant = projectParticipantRepository
                .findByProjectIdAndUserId(projectId, userInfo.userId())
                .orElseThrow(() -> new ProjectNotFoundException(projectId));

        List<ProjectParticipant> participants = projectParticipantRepository
                .findByProjectIdWithUserAndProject(projectId);
        List<DatasetItem> datasetItems = datasetItemRepository.findByProjectIdOrderByItemIndexAsc(projectId);

        DatasetItem targetItem = datasetItems.stream()
                .filter(datasetItem -> datasetItem.getId().equals(request.datasetItemId()))
                .findFirst()
                .orElseThrow(() -> new InvalidProjectDatasetException("Dataset item does not belong to this project"));

        long totalSteps = countTotalSteps(datasetItems);
        long completedStepsBeforeSave = Math.max(0L,
                Math.min(annotationRepository.countByDatasetItemProjectIdAndUserId(projectId, userInfo.userId()), totalSteps));
        int completionBeforeSave = ProjectAnnotationUtils.toPercentage(completedStepsBeforeSave, totalSteps);

        Object rawAnnotation = request.annotation();
        Object normalizedAnnotation = rawAnnotation;
        if (rawAnnotation != null) {
            normalizedAnnotation = annotationPayloadNormalizer.normalize(
                    rawAnnotation,
                    participant.getProject().getProjectType());
        }
        ProjectDatasetUtils.DatasetStepDefinition stepDefinition = ProjectDatasetUtils
                .resolveStepDefinition(targetItem);

        if (stepDefinition.totalSteps() <= 0) {
            throw new InvalidProjectDatasetException("Dataset item has no annotatable steps");
        }

        int stepIndex = annotationStepResolver.resolveStepIndex(request.stepIndex(), stepDefinition.totalSteps());

        if (normalizedAnnotation == null) {
            removeStepAnnotation(targetItem, userInfo.userId(), stepIndex);
        } else {
            storeStepAnnotation(targetItem, userInfo.userId(), stepIndex, normalizedAnnotation);
        }

        Map<Long, Long> completedStepsAfterSave = projectProgressCalculator.buildCompletedStepCountMap(projectId);
        ProjectProgressSnapshot progressAfterSave = projectProgressCalculator
                .buildProjectProgressSnapshot(participants, totalSteps, completedStepsAfterSave);
        int completionAfterSave = progressAfterSave.completionPercentageForUser(userInfo.userId());

        if (completionBeforeSave < 100 && completionAfterSave == 100) {
            notifyProjectOwnersOnAnnotationCompletion(participant.getProject(), participants, userInfo);
        }
        projectMetricsCacheService.evictProjectReadCaches(projectId);

        return new SaveProjectAnnotationStepResponseDto(
                projectId,
                targetItem.getId(),
                stepIndex,
                progressAfterSave.completedStepsForUser(userInfo.userId()),
                progressAfterSave.totalSteps(),
                completionAfterSave,
                progressAfterSave.projectCompletionPercentage());
    }

    @Override
    @Transactional
    public SaveProjectAnnotationStepResponseDto toggleAnnotationWarning(String authenticatedEmail, Long projectId,
            Long participantUserId, Long datasetItemId, Integer stepIndex) {
        UserInfo requesterInfo = authApiService.findUserByEmail(authenticatedEmail);

        ProjectParticipant requesterParticipant = projectParticipantRepository
                .findByProjectIdAndUserId(projectId, requesterInfo.userId())
                .orElseThrow(() -> new ProjectNotFoundException(projectId));

        annotationWarningPolicy.requireCreator(requesterParticipant);

        ProjectParticipant targetParticipant = projectParticipantRepository
                .findByProjectIdAndUserId(projectId, participantUserId)
                .orElseThrow(() -> new InvalidProjectParticipantsException(
                        "Selected investigator is not assigned to this project"));

        List<ProjectParticipant> participants = projectParticipantRepository
                .findByProjectIdWithUserAndProject(projectId);
        List<DatasetItem> datasetItems = datasetItemRepository.findByProjectIdOrderByItemIndexAsc(projectId);

        Annotation annotation = annotationRepository
                .findByDatasetItemIdAndUserIdAndStepIndex(datasetItemId, participantUserId, stepIndex)
                .orElseThrow(() -> new InvalidProjectDatasetException("Annotation not found for this step"));

        boolean newWarningStatus = !annotation.isWarning();
        annotation.setWarning(newWarningStatus);
        annotationRepository.save(annotation);

        if (newWarningStatus) {
            notificationService.createProjectAnnotationWarningNotification(
                    participantUserId, requesterInfo.userId(),
                    projectId, requesterParticipant.getProject().getName(),
                    requesterParticipant.getProject().getResearchGroup().getId(),
                    requesterParticipant.getProject().getResearchGroup().getName());
        }
        projectMetricsCacheService.evictProjectReadCaches(projectId);

        ProjectProgressSnapshot progressSnapshot = projectProgressCalculator
                .buildProjectProgressSnapshotForDatasetItems(projectId, participants, datasetItems);

        return new SaveProjectAnnotationStepResponseDto(
                projectId,
                datasetItemId,
                stepIndex,
                progressSnapshot.completedStepsForUser(participantUserId),
                progressSnapshot.totalSteps(),
                progressSnapshot.completionPercentageForUser(participantUserId),
                progressSnapshot.projectCompletionPercentage());
    }

    private ProjectAnnotationWorkspaceDto buildAnnotationWorkspace(
            Project project, Long annotationUserId, int offset, int limit) {
        Long projectId = project.getId();

        List<DatasetItem> datasetItems = datasetItemRepository.findByProjectIdOrderByItemIndexAsc(projectId);
        List<ProjectParticipant> participants = projectParticipantRepository.findByProjectIdWithUserAndProject(
                projectId);
        Map<Long, Map<Integer, Annotation>> annotationLookup = buildUserAnnotationLookup(projectId, annotationUserId);
        ProjectProgressSnapshot progressSnapshot = projectProgressCalculator
                .buildProjectProgressSnapshotForDatasetItems(projectId, participants, datasetItems);

        int sanitizedOffset = Math.max(offset, 0);
        int sanitizedLimit = sanitizeAnnotationStepsLimit(limit);

        return new ProjectAnnotationWorkspaceDto(
                projectId,
                project.getProjectType(),
                project.getAnnotationTargetColumn(),
                project.getLabels().stream()
                        .map(label -> new ProjectSetupLabelDto(label.getName(), label.getColor()))
                        .toList(),
                sanitizedOffset,
                sanitizedLimit,
                progressSnapshot.totalSteps(),
                progressSnapshot.completedStepsForUser(annotationUserId),
                progressSnapshot.completionPercentageForUser(annotationUserId),
                findFirstPendingStepIndex(datasetItems, annotationLookup),
                buildAnnotationSteps(datasetItems, sanitizedOffset, sanitizedLimit, annotationLookup));
    }

    private int sanitizeAnnotationStepsLimit(int limit) {
        if (limit <= 0) {
            return ProjectConstants.DEFAULT_ANNOTATION_STEPS_LIMIT;
        }
        return Math.min(limit, ProjectConstants.MAX_ANNOTATION_STEPS_LIMIT);
    }

    private List<ProjectAnnotationStepDto> buildAnnotationSteps(
            List<DatasetItem> datasetItems, int offset, int limit,
            Map<Long, Map<Integer, Annotation>> annotationLookup) {
        List<ProjectAnnotationStepDto> steps = new ArrayList<>();
        long absoluteIndex = 0;

        for (DatasetItem datasetItem : datasetItems) {
            ProjectDatasetUtils.DatasetStepDefinition definition = ProjectDatasetUtils
                    .resolveStepDefinition(datasetItem);
            int totalSteps = definition.totalSteps();
            if (totalSteps <= 0) continue;

            for (int stepIndex = 0; stepIndex < totalSteps; stepIndex++) {
                if (absoluteIndex++ < offset) continue;
                if (steps.size() >= limit) return steps;

                Annotation annotation = findStepAnnotation(annotationLookup, datasetItem.getId(), stepIndex);
                Object payload = extractNormalizedPayload(annotation);

                steps.add(new ProjectAnnotationStepDto(
                        datasetItem.getId(), datasetItem.getItemIndex(), stepIndex, totalSteps,
                        ProjectDatasetUtils.valueAsString(
                                datasetItem.getContent().get(ProjectConstants.CONTENT_KEY_FILE_NAME)),
                        ProjectDatasetUtils.valueAsString(
                                datasetItem.getContent().get(ProjectConstants.CONTENT_KEY_MIME_TYPE)),
                        definition.previewForStep(stepIndex),
                        definition.rowValuesForStep(stepIndex),
                        ProjectAnnotationUtils.hasAnnotationPayload(payload),
                        annotation != null && annotation.isWarning(),
                        payload));
            }
        }
        return steps;
    }

    private int findFirstPendingStepIndex(List<DatasetItem> datasetItems,
            Map<Long, Map<Integer, Annotation>> annotationLookup) {
        int globalStepIndex = 1;
        for (DatasetItem datasetItem : datasetItems) {
            ProjectDatasetUtils.DatasetStepDefinition stepDefinition = ProjectDatasetUtils
                    .resolveStepDefinition(datasetItem);
            int totalStepsForItem = stepDefinition.totalSteps();
            for (int stepIndex = 0; stepIndex < totalStepsForItem; stepIndex++) {
                Annotation annotation = findStepAnnotation(annotationLookup, datasetItem.getId(), stepIndex);
                Object payload = extractNormalizedPayload(annotation);
                if (!ProjectAnnotationUtils.hasAnnotationPayload(payload)) {
                    return globalStepIndex;
                }
                globalStepIndex++;
            }
        }
        return globalStepIndex > 1 ? 1 : 0;
    }

    private long countTotalSteps(List<DatasetItem> datasetItems) {
        long totalSteps = 0L;
        for (DatasetItem datasetItem : datasetItems) {
            totalSteps += ProjectDatasetUtils.resolveStepDefinition(datasetItem).totalSteps();
        }
        return totalSteps;
    }

    private Map<Long, Map<Integer, Annotation>> buildUserAnnotationLookup(Long projectId, Long userId) {
        List<Annotation> annotations = annotationRepository.findByProjectIdAndUserIdWithDatasetItem(projectId, userId);
        Map<Long, Map<Integer, Annotation>> annotationLookup = new LinkedHashMap<>();
        for (Annotation annotation : annotations) {
            if (annotation.getDatasetItem() == null || annotation.getDatasetItem().getId() == null
                    || annotation.getStepIndex() == null) {
                continue;
            }
            annotationLookup
                    .computeIfAbsent(annotation.getDatasetItem().getId(), ignored -> new LinkedHashMap<>())
                    .put(annotation.getStepIndex(), annotation);
        }
        return annotationLookup;
    }

    private void storeStepAnnotation(DatasetItem datasetItem, Long userId, int stepIndex, Object annotationPayload) {
        Map<String, Object> annotationMap = ProjectAnnotationUtils.normalizeAnnotationAsMap(annotationPayload);
        Annotation annotation = annotationRepository
                .findByDatasetItemIdAndUserIdAndStepIndex(datasetItem.getId(), userId, stepIndex)
                .orElseGet(() -> {
                    Annotation created = new Annotation();
                    created.setDatasetItem(datasetItem);
                    entityReferenceService.attachUser(created, userId);
                    created.setStepIndex(stepIndex);
                    return created;
                });
        annotation.setPayload(annotationMap);
        annotationRepository.save(annotation);
    }

    private void removeStepAnnotation(DatasetItem datasetItem, Long userId, int stepIndex) {
        annotationRepository.deleteByDatasetItemIdAndUserIdAndStepIndex(datasetItem.getId(), userId, stepIndex);
    }

    private Annotation findStepAnnotation(Map<Long, Map<Integer, Annotation>> annotationLookup,
            Long datasetItemId, int stepIndex) {
        Map<Integer, Annotation> stepsByDatasetItem = annotationLookup.get(datasetItemId);
        if (stepsByDatasetItem == null) return null;
        return stepsByDatasetItem.get(stepIndex);
    }

    private Object extractNormalizedPayload(Annotation annotation) {
        if (annotation == null) return null;
        Object annotationPayload = annotation.getPayload();
        if (!(annotationPayload instanceof Map<?, ?> rawMap)
                || !rawMap.containsKey(ProjectConstants.ANNOTATION_WRAPPED_VALUE_KEY)
                || rawMap.size() != 1) {
            return annotationPayload;
        }
        return rawMap.get(ProjectConstants.ANNOTATION_WRAPPED_VALUE_KEY);
    }

    private void notifyProjectOwnersOnAnnotationCompletion(Project project, List<ProjectParticipant> participants,
            UserInfo actor) {
        for (ProjectParticipant projectParticipant : participants) {
            if (projectParticipant.getRole() != ProjectParticipantRole.CREATOR) continue;
            Long ownerId = projectParticipant.getUser().getId();
            if (ownerId.equals(actor.userId())) continue;

            notificationService.createProjectAnnotationCompletedNotification(
                    ownerId, actor.userId(),
                    project.getId(), project.getName(),
                    project.getResearchGroup().getId(), project.getResearchGroup().getName());
        }
    }

}
