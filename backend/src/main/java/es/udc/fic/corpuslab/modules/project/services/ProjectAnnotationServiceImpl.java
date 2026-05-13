package es.udc.fic.corpuslab.modules.project.services;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;

import es.udc.fic.corpuslab.common.utils.StringUtils;
import es.udc.fic.corpuslab.modules.auth.api.AuthApiService;
import es.udc.fic.corpuslab.modules.auth.api.dtos.UserInfo;
import es.udc.fic.corpuslab.modules.auth.entities.User;
import es.udc.fic.corpuslab.modules.notification.services.NotificationService;
import es.udc.fic.corpuslab.modules.project.dtos.ProjectAnnotationStepDto;
import es.udc.fic.corpuslab.modules.project.dtos.ProjectAnnotationWorkspaceDto;
import es.udc.fic.corpuslab.modules.project.dtos.ProjectSetupLabelDto;
import es.udc.fic.corpuslab.modules.project.dtos.SaveProjectAnnotationStepRequestDto;
import es.udc.fic.corpuslab.modules.project.dtos.SaveProjectAnnotationStepResponseDto;
import es.udc.fic.corpuslab.modules.project.dtos.UserAnnotationCountDto;
import es.udc.fic.corpuslab.modules.project.entities.Annotation;
import es.udc.fic.corpuslab.modules.project.entities.DatasetItem;
import es.udc.fic.corpuslab.modules.project.entities.Project;
import es.udc.fic.corpuslab.modules.project.entities.ProjectParticipant;
import es.udc.fic.corpuslab.modules.project.enums.ProjectParticipantRole;
import es.udc.fic.corpuslab.modules.project.enums.ProjectType;
import es.udc.fic.corpuslab.modules.project.exceptions.InvalidProjectDatasetException;
import es.udc.fic.corpuslab.modules.project.exceptions.InvalidProjectParticipantsException;
import es.udc.fic.corpuslab.modules.project.exceptions.ProjectNotFoundException;
import es.udc.fic.corpuslab.modules.project.repositories.AnnotationRepository;
import es.udc.fic.corpuslab.modules.project.repositories.DatasetItemRepository;
import es.udc.fic.corpuslab.modules.project.repositories.ProjectParticipantRepository;
import es.udc.fic.corpuslab.modules.project.utils.ProjectAnnotationUtils;
import es.udc.fic.corpuslab.modules.project.utils.ProjectConstants;
import es.udc.fic.corpuslab.modules.project.utils.ProjectDatasetUtils;
import jakarta.persistence.EntityManager;

@Service
public class ProjectAnnotationServiceImpl implements ProjectAnnotationService {

    private final ProjectParticipantRepository projectParticipantRepository;
    private final DatasetItemRepository datasetItemRepository;
    private final AnnotationRepository annotationRepository;
    private final AuthApiService authApiService;
    private final NotificationService notificationService;
    private final ProjectMetricsCacheService projectMetricsCacheService;
    private final EntityManager entityManager;

    @Autowired
    public ProjectAnnotationServiceImpl(
            ProjectParticipantRepository projectParticipantRepository,
            DatasetItemRepository datasetItemRepository,
            AnnotationRepository annotationRepository,
            AuthApiService authApiService,
            NotificationService notificationService,
            ProjectMetricsCacheService projectMetricsCacheService,
            EntityManager entityManager) {
        this.projectParticipantRepository = projectParticipantRepository;
        this.datasetItemRepository = datasetItemRepository;
        this.annotationRepository = annotationRepository;
        this.authApiService = authApiService;
        this.notificationService = notificationService;
        this.projectMetricsCacheService = projectMetricsCacheService;
        this.entityManager = entityManager;
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
            normalizedAnnotation = normalizeAnnotationPayload(rawAnnotation);
            if (participant.getProject().getProjectType() == ProjectType.NER) {
                normalizedAnnotation = ProjectAnnotationUtils.normalizeNerAnnotationPayload(normalizedAnnotation);
            }
        }
        ProjectDatasetUtils.DatasetStepDefinition stepDefinition = ProjectDatasetUtils
                .resolveStepDefinition(targetItem);

        if (stepDefinition.totalSteps() <= 0) {
            throw new InvalidProjectDatasetException("Dataset item has no annotatable steps");
        }

        int stepIndex = resolveStepIndex(request.stepIndex(), stepDefinition.totalSteps());

        User userRef = entityManager.getReference(User.class, userInfo.userId());

        if (normalizedAnnotation == null) {
            removeStepAnnotation(targetItem, userRef, stepIndex);
        } else {
            storeStepAnnotation(targetItem, userRef, stepIndex, normalizedAnnotation);
        }

        Map<Long, Long> completedStepsAfterSave = buildCompletedStepCountMap(projectId);
        ProjectAnnotationUtils.ProjectProgressSnapshot progressAfterSave = buildProjectProgressSnapshot(
                participants, datasetItems, completedStepsAfterSave);
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

        if (requesterParticipant.getRole() != ProjectParticipantRole.CREATOR) {
            throw new AccessDeniedException("Only project creators can toggle annotation warnings");
        }

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

        ProjectAnnotationUtils.ProjectProgressSnapshot progressSnapshot = buildProjectProgressSnapshot(
                participants,
                datasetItems,
                buildCompletedStepCountMap(projectId));

        return new SaveProjectAnnotationStepResponseDto(
                projectId,
                datasetItemId,
                stepIndex,
                progressSnapshot.completedStepsForUser(participantUserId),
                progressSnapshot.totalSteps(),
                progressSnapshot.completionPercentageForUser(participantUserId),
                progressSnapshot.projectCompletionPercentage());
    }

    @Override
    @Transactional(readOnly = true)
    public String getAnnotationResultsCsvFileName(String authenticatedEmail, Long projectId) {
        return buildAnnotationExportFileName(findExportableProject(authenticatedEmail, projectId));
    }

    @Override
    @Transactional(readOnly = true)
    public void writeAnnotationResultsCsv(String authenticatedEmail, Long projectId, OutputStream outputStream)
            throws IOException {
        writeAnnotationResultsCsv(prepareAnnotationExport(authenticatedEmail, projectId), outputStream);
    }

    private CsvExportContext prepareAnnotationExport(String authenticatedEmail, Long projectId) {
        Project project = findExportableProject(authenticatedEmail, projectId);
        List<DatasetItem> datasetItems = datasetItemRepository.findByProjectIdOrderByItemIndexAsc(projectId);
        Map<Long, Map<Long, Map<Integer, Annotation>>> annotationLookup = buildAnnotationLookup(projectId);
        List<ProjectAnnotationUtils.AnnotatorExportColumn> annotatorColumns = buildAnnotatorExportColumns(projectId);

        LinkedHashSet<String> csvColumns = collectCsvColumns(datasetItems);
        List<ProjectAnnotationUtils.AnnotatorExportColumn> nonEmptyAnnotatorColumns = buildNonEmptyAnnotatorColumns(
                datasetItems,
                annotationLookup,
                annotatorColumns);

        return new CsvExportContext(
                project,
                datasetItems,
                annotationLookup,
                csvColumns,
                nonEmptyAnnotatorColumns,
                datasetItems.stream().anyMatch(ProjectDatasetUtils::isCsvDatasetItem));
    }

    private Project findExportableProject(String authenticatedEmail, Long projectId) {
        UserInfo requesterInfo = authApiService.findUserByEmail(authenticatedEmail);

        ProjectParticipant requesterParticipant = projectParticipantRepository
                .findByProjectIdAndUserId(projectId, requesterInfo.userId())
                .orElseThrow(() -> new ProjectNotFoundException(projectId));

        if (requesterParticipant.getRole() != ProjectParticipantRole.CREATOR) {
            throw new AccessDeniedException("Only project creators can export annotation results");
        }

        return requesterParticipant.getProject();
    }

    private LinkedHashSet<String> collectCsvColumns(List<DatasetItem> datasetItems) {
        LinkedHashSet<String> csvColumns = new LinkedHashSet<>();
        for (DatasetItem datasetItem : datasetItems) {
            ProjectDatasetUtils.DatasetStepDefinition stepDefinition = ProjectDatasetUtils
                    .resolveStepDefinition(datasetItem);
            for (int stepIndex = 0; stepIndex < stepDefinition.totalSteps(); stepIndex++) {
                Map<String, String> rowValues = stepDefinition.rowValuesForStep(stepIndex);
                if (rowValues != null && !rowValues.isEmpty()) {
                    csvColumns.addAll(rowValues.keySet());
                }
            }
        }
        return csvColumns;
    }

    private List<ProjectAnnotationUtils.AnnotatorExportColumn> buildNonEmptyAnnotatorColumns(
            List<DatasetItem> datasetItems,
            Map<Long, Map<Long, Map<Integer, Annotation>>> annotationLookup,
            List<ProjectAnnotationUtils.AnnotatorExportColumn> annotatorColumns) {
        List<ProjectAnnotationUtils.AnnotatorExportColumn> nonEmptyAnnotatorColumns = new ArrayList<>();
        for (ProjectAnnotationUtils.AnnotatorExportColumn annotatorColumn : annotatorColumns) {
            boolean hasAnnotation = false;
            boolean hasComment = false;

            for (DatasetItem datasetItem : datasetItems) {
                ProjectDatasetUtils.DatasetStepDefinition stepDefinition = ProjectDatasetUtils
                        .resolveStepDefinition(datasetItem);
                for (int stepIndex = 0; stepIndex < stepDefinition.totalSteps(); stepIndex++) {
                    Annotation annotation = findStepAnnotation(
                            annotationLookup,
                            datasetItem.getId(),
                            annotatorColumn.userId(),
                            stepIndex);
                    Object annotationPayload = annotation == null ? null : annotation.getPayload();
                    if (!hasAnnotation && !extractAnnotationValue(annotationPayload).isEmpty()) {
                        hasAnnotation = true;
                    }
                    if (!hasComment && !extractCommentValue(annotationPayload).isEmpty()) {
                        hasComment = true;
                    }
                    if (hasAnnotation && hasComment) {
                        break;
                    }
                }
                if (hasAnnotation && hasComment) {
                    break;
                }
            }

            if (hasAnnotation || hasComment) {
                nonEmptyAnnotatorColumns.add(new ProjectAnnotationUtils.AnnotatorExportColumn(
                        annotatorColumn.userId(),
                        annotatorColumn.annotationHeader(),
                        annotatorColumn.commentHeader(),
                        hasAnnotation,
                        hasComment));
            }
        }
        return nonEmptyAnnotatorColumns;
    }

    private void writeAnnotationResultsCsv(CsvExportContext context, OutputStream outputStream) throws IOException {
        Writer writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
        appendCsvLine(writer, buildExportHeaders(context));

        for (DatasetItem datasetItem : context.datasetItems()) {
            ProjectDatasetUtils.DatasetStepDefinition stepDefinition = ProjectDatasetUtils
                    .resolveStepDefinition(datasetItem);
            int totalSteps = stepDefinition.totalSteps();

            for (int stepIndex = 0; stepIndex < totalSteps; stepIndex++) {
                Map<String, String> rowValues = stepDefinition.rowValuesForStep(stepIndex);
                List<String> values = new ArrayList<>();

                if (!context.isCsvDataset()) {
                    values.add(String.valueOf(datasetItem.getItemIndex()));
                    values.add(ProjectDatasetUtils
                            .valueAsString(datasetItem.getContent().get(ProjectConstants.CONTENT_KEY_FILE_NAME)));
                }

                for (String csvColumn : context.csvColumns()) {
                    values.add(rowValues == null ? "" : rowValues.getOrDefault(csvColumn, ""));
                }

                for (ProjectAnnotationUtils.AnnotatorExportColumn annotatorColumn : context
                        .nonEmptyAnnotatorColumns()) {
                    Annotation annotation = findStepAnnotation(
                            context.annotationLookup(),
                            datasetItem.getId(),
                            annotatorColumn.userId(),
                            stepIndex);
                    Object annotationPayload = annotation == null ? null : annotation.getPayload();
                    if (annotatorColumn.hasAnnotation()) {
                        values.add(extractAnnotationValue(annotationPayload));
                    }
                    if (annotatorColumn.hasComment()) {
                        values.add(extractCommentValue(annotationPayload));
                    }
                }

                appendCsvLine(writer, values);
            }
        }
        writer.flush();
    }

    private List<String> buildExportHeaders(CsvExportContext context) {
        List<String> headers = new ArrayList<>();
        if (!context.isCsvDataset()) {
            headers.add("dataset_item_index");
            headers.add("source_name");
        }
        headers.addAll(context.csvColumns());

        for (ProjectAnnotationUtils.AnnotatorExportColumn annotatorColumn : context.nonEmptyAnnotatorColumns()) {
            if (annotatorColumn.hasAnnotation()) {
                headers.add(annotatorColumn.annotationHeader());
            }
            if (annotatorColumn.hasComment()) {
                headers.add(annotatorColumn.commentHeader());
            }
        }
        return headers;
    }

    private ProjectAnnotationWorkspaceDto buildAnnotationWorkspace(
            Project project, Long annotationUserId, int offset, int limit) {
        Long projectId = project.getId();

        List<DatasetItem> datasetItems = datasetItemRepository.findByProjectIdOrderByItemIndexAsc(projectId);
        List<ProjectParticipant> participants = projectParticipantRepository.findByProjectIdWithUserAndProject(
                projectId);
        Map<Long, Map<Integer, Annotation>> annotationLookup = buildUserAnnotationLookup(projectId, annotationUserId);
        ProjectAnnotationUtils.ProjectProgressSnapshot progressSnapshot = buildProjectProgressSnapshot(
                participants,
                datasetItems,
                buildCompletedStepCountMap(projectId));

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

    private ProjectAnnotationUtils.ProjectProgressSnapshot buildProjectProgressSnapshot(
            List<ProjectParticipant> participants,
            List<DatasetItem> datasetItems,
            Map<Long, Long> completedStepCounts) {
        long totalSteps = countTotalSteps(datasetItems);
        Map<Long, Long> completedStepsByUser = new LinkedHashMap<>();
        Map<Long, Integer> completionPercentageByUser = new LinkedHashMap<>();

        for (ProjectParticipant participant : participants) {
            if (participant.getUser() == null || participant.getUser().getId() == null) {
                continue;
            }
            Long userId = participant.getUser().getId();
            long completedSteps = Math.max(0L, Math.min(
                    completedStepCounts.getOrDefault(userId, 0L),
                    totalSteps));
            completedStepsByUser.put(userId, completedSteps);
            completionPercentageByUser.put(userId, ProjectAnnotationUtils.toPercentage(completedSteps, totalSteps));
        }

        long totalCompletedSteps = completedStepsByUser.values().stream()
                .mapToLong(Long::longValue)
                .sum();
        long totalPossibleSteps = totalSteps * completedStepsByUser.size();

        return new ProjectAnnotationUtils.ProjectProgressSnapshot(
                totalSteps,
                ProjectAnnotationUtils.toPercentage(totalCompletedSteps, totalPossibleSteps),
                completedStepsByUser,
                completionPercentageByUser);
    }

    private long countTotalSteps(List<DatasetItem> datasetItems) {
        long totalSteps = 0L;
        for (DatasetItem datasetItem : datasetItems) {
            totalSteps += ProjectDatasetUtils.resolveStepDefinition(datasetItem).totalSteps();
        }
        return totalSteps;
    }

    private Map<Long, Long> buildCompletedStepCountMap(Long projectId) {
        List<UserAnnotationCountDto> counts = annotationRepository.countCompletedStepsByUser(projectId);
        Map<Long, Long> result = new LinkedHashMap<>();
        for (UserAnnotationCountDto count : counts) {
            if (count.userId() != null) {
                result.put(count.userId(), count.completedSteps());
            }
        }
        return result;
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

    private Map<Long, Map<Long, Map<Integer, Annotation>>> buildAnnotationLookup(Long projectId) {
        List<Annotation> annotations = annotationRepository.findByProjectIdWithDatasetItemAndUser(projectId);
        Map<Long, Map<Long, Map<Integer, Annotation>>> annotationLookup = new LinkedHashMap<>();
        for (Annotation annotation : annotations) {
            if (annotation.getDatasetItem() == null || annotation.getUser() == null) continue;
            Long datasetItemId = annotation.getDatasetItem().getId();
            Long userId = annotation.getUser().getId();
            Integer stepIndex = annotation.getStepIndex();
            if (datasetItemId == null || userId == null || stepIndex == null) continue;
            annotationLookup
                    .computeIfAbsent(datasetItemId, ignored -> new LinkedHashMap<>())
                    .computeIfAbsent(userId, ignored -> new LinkedHashMap<>())
                    .put(stepIndex, annotation);
        }
        return annotationLookup;
    }

    private Object normalizeAnnotationPayload(Object annotationPayload) {
        if (annotationPayload == null) {
            throw new InvalidProjectDatasetException("Annotation payload is required");
        }
        if (annotationPayload instanceof String s && s.trim().isEmpty()) {
            throw new InvalidProjectDatasetException("Annotation payload is required");
        }
        if (annotationPayload instanceof List<?> l && l.isEmpty()) {
            throw new InvalidProjectDatasetException("Annotation payload is required");
        }
        if (annotationPayload instanceof Map<?, ?> m && m.isEmpty()) {
            throw new InvalidProjectDatasetException("Annotation payload is required");
        }
        return annotationPayload;
    }

    private int resolveStepIndex(Integer requestedStepIndex, int totalSteps) {
        int stepIndex = requestedStepIndex == null ? 0 : requestedStepIndex;
        if (stepIndex < 0 || stepIndex >= totalSteps) {
            throw new InvalidProjectDatasetException("Invalid annotation step index");
        }
        return stepIndex;
    }

    private void storeStepAnnotation(DatasetItem datasetItem, User user, int stepIndex, Object annotationPayload) {
        Map<String, Object> annotationMap = ProjectAnnotationUtils.normalizeAnnotationAsMap(annotationPayload);
        Annotation annotation = annotationRepository
                .findByDatasetItemIdAndUserIdAndStepIndex(datasetItem.getId(), user.getId(), stepIndex)
                .orElseGet(() -> {
                    Annotation created = new Annotation();
                    created.setDatasetItem(datasetItem);
                    created.setUser(user);
                    created.setStepIndex(stepIndex);
                    return created;
                });
        annotation.setPayload(annotationMap);
        annotationRepository.save(annotation);
    }

    private void removeStepAnnotation(DatasetItem datasetItem, User user, int stepIndex) {
        annotationRepository.deleteByDatasetItemIdAndUserIdAndStepIndex(datasetItem.getId(), user.getId(), stepIndex);
    }

    private Annotation findStepAnnotation(Map<Long, Map<Integer, Annotation>> annotationLookup,
            Long datasetItemId, int stepIndex) {
        Map<Integer, Annotation> stepsByDatasetItem = annotationLookup.get(datasetItemId);
        if (stepsByDatasetItem == null) return null;
        return stepsByDatasetItem.get(stepIndex);
    }

    private Annotation findStepAnnotation(Map<Long, Map<Long, Map<Integer, Annotation>>> annotationLookup,
            Long datasetItemId, Long userId, int stepIndex) {
        Map<Long, Map<Integer, Annotation>> userAnnotationsByDataset = annotationLookup.get(datasetItemId);
        if (userAnnotationsByDataset == null) return null;
        Map<Integer, Annotation> stepsByUser = userAnnotationsByDataset.get(userId);
        if (stepsByUser == null) return null;
        return stepsByUser.get(stepIndex);
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

    private void appendCsvLine(Writer writer, List<String> values) throws IOException {
        for (int index = 0; index < values.size(); index++) {
            if (index > 0) writer.append(',');
            writer.append(escapeCsvValue(values.get(index)));
        }
        writer.append('\n');
    }

    private String escapeCsvValue(String value) {
        if (value == null) return "";
        boolean mustBeQuoted = value.contains(",") || value.contains("\"")
                || value.contains("\n") || value.contains("\r");
        if (!mustBeQuoted) return value;
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private String buildAnnotationExportFileName(Project project) {
        String projectName = StringUtils.trimToNull(project.getName());
        if (projectName == null) return "project-" + project.getId() + "-annotations.csv";
        String slugifiedName = projectName.toLowerCase().replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+)|(-+$)", "");
        if (slugifiedName.isBlank()) return "project-" + project.getId() + "-annotations.csv";
        return slugifiedName + "-annotations.csv";
    }

    private List<ProjectAnnotationUtils.AnnotatorExportColumn> buildAnnotatorExportColumns(Long projectId) {
        Map<Long, String> annotatorIdByUserId = new LinkedHashMap<>();
        for (ProjectParticipant pp : projectParticipantRepository.findByProjectIdWithUserAndProject(projectId)) {
            User annotator = pp.getUser();
            if (annotator == null || annotator.getId() == null) continue;
            annotatorIdByUserId.putIfAbsent(annotator.getId(), normalizeExportAnnotatorId(annotator));
        }
        return annotatorIdByUserId.entrySet().stream()
                .map(entry -> new ProjectAnnotationUtils.AnnotatorExportColumn(
                        entry.getKey(),
                        entry.getValue() + ProjectConstants.EXPORT_ANNOTATION_HEADER_SUFFIX,
                        entry.getValue() + ProjectConstants.EXPORT_COMMENT_HEADER_SUFFIX,
                        true,
                        true))
                .toList();
    }

    private String normalizeExportAnnotatorId(User annotator) {
        return annotator.getId().toString();
    }

    private String extractAnnotationValue(Object annotationPayload) {
        if (!ProjectAnnotationUtils.hasAnnotationPayload(annotationPayload)) return "";
        String normalizedBoolean = normalizeBooleanValue(annotationPayload);
        if (normalizedBoolean != null) return normalizedBoolean;
        if (annotationPayload instanceof String s) return s.trim();
        if (annotationPayload instanceof Number || annotationPayload instanceof Boolean)
            return String.valueOf(annotationPayload);
        if (annotationPayload instanceof List<?> l) return normalizeListLikeValue(l);
        if (!(annotationPayload instanceof Map<?, ?> annotationAsMap)) return String.valueOf(annotationPayload);

        Map<String, Object> annotationMap = ProjectAnnotationUtils.toMutableStringObjectMap(annotationAsMap);
        String labelValue = StringUtils.trimToNull(
                ProjectDatasetUtils.valueAsString(annotationMap.get(ProjectConstants.ANNOTATION_KEY_LABEL)));
        if (labelValue != null) return labelValue;
        String labelsValue = normalizeListLikeValue(annotationMap.get(ProjectConstants.ANNOTATION_KEY_LABELS));
        if (!labelsValue.isBlank()) return labelsValue;
        String textValue = StringUtils.trimToNull(
                ProjectDatasetUtils.valueAsString(annotationMap.get(ProjectConstants.ANNOTATION_KEY_TEXT)));
        if (textValue != null) return textValue;
        String entitiesValue = normalizeNerEntitiesForExport(
                annotationMap.get(ProjectConstants.NER_ANNOTATION_KEY_ENTITIES));
        if (!entitiesValue.isBlank()) return entitiesValue;
        String binaryValue = normalizeBooleanValue(
                annotationMap.get(ProjectConstants.ANNOTATION_KEY_BINARY_VALUE));
        if (binaryValue != null) return binaryValue;
        return annotationMap.toString();
    }

    private String extractCommentValue(Object annotationPayload) {
        if (!(annotationPayload instanceof Map<?, ?> annotationAsMap)) return "";
        Map<String, Object> annotationMap = ProjectAnnotationUtils.toMutableStringObjectMap(annotationAsMap);
        String notes = StringUtils.trimToNull(
                ProjectDatasetUtils.valueAsString(annotationMap.get(ProjectConstants.ANNOTATION_KEY_NOTES)));
        return notes == null ? "" : notes;
    }

    private String normalizeNerEntitiesForExport(Object rawEntities) {
        if (!(rawEntities instanceof List<?> entities)) return "";
        List<String> normalizedEntities = entities.stream().map(rawEntity -> {
            if (!(rawEntity instanceof Map<?, ?> rawEntityMap)) return null;
            Map<String, Object> entityMap = ProjectAnnotationUtils.toMutableStringObjectMap(rawEntityMap);
            String label = StringUtils.trimToNull(
                    ProjectDatasetUtils.valueAsString(entityMap.get(ProjectConstants.NER_ANNOTATION_KEY_LABEL)));
            String text = StringUtils.trimToNull(
                    ProjectDatasetUtils.valueAsString(entityMap.get(ProjectConstants.NER_ANNOTATION_KEY_TEXT)));
            if (label != null && text != null) return label + ":" + text;
            if (text != null) return text;
            return label;
        }).filter(v -> v != null && !v.isBlank()).distinct().toList();
        return String.join("|", normalizedEntities);
    }

    private String normalizeListLikeValue(Object value) {
        if (value == null) return "";
        if (value instanceof String s) { String n = s.trim(); return n.isEmpty() ? "" : n; }
        if (!(value instanceof List<?> l)) return "";
        List<String> normalized = l.stream().filter(e -> e != null).map(String::valueOf)
                .map(String::trim).filter(e -> !e.isEmpty()).distinct().toList();
        return String.join("|", normalized);
    }

    private String normalizeBooleanValue(Object value) {
        if (value instanceof Boolean b) return String.valueOf(b);
        if (value instanceof Number n) {
            long l = n.longValue();
            if (l == 0L || l == 1L) return String.valueOf(l == 1L);
            return null;
        }
        if (!(value instanceof String s)) return null;
        String n = s.trim().toLowerCase();
        if (n.equals("true") || n.equals("false")) return n;
        return null;
    }

    private record CsvExportContext(
            Project project,
            List<DatasetItem> datasetItems,
            Map<Long, Map<Long, Map<Integer, Annotation>>> annotationLookup,
            LinkedHashSet<String> csvColumns,
            List<ProjectAnnotationUtils.AnnotatorExportColumn> nonEmptyAnnotatorColumns,
            boolean isCsvDataset) {
    }
}       
