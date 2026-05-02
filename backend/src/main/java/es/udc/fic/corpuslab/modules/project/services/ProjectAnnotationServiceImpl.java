package es.udc.fic.corpuslab.modules.project.services;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;

import es.udc.fic.corpuslab.common.utils.StringUtils;
import es.udc.fic.corpuslab.modules.auth.api.AuthApiService;
import es.udc.fic.corpuslab.modules.auth.api.dtos.UserInfo;
import es.udc.fic.corpuslab.modules.auth.entities.User;
import es.udc.fic.corpuslab.modules.notification.services.NotificationService;
import es.udc.fic.corpuslab.modules.project.dtos.ProjectAnnotationExportCsvDto;
import es.udc.fic.corpuslab.modules.project.dtos.ProjectAnnotationStepDto;
import es.udc.fic.corpuslab.modules.project.dtos.ProjectAnnotationWorkspaceDto;
import es.udc.fic.corpuslab.modules.project.dtos.ProjectSetupLabelDto;
import es.udc.fic.corpuslab.modules.project.dtos.SaveProjectAnnotationStepRequestDto;
import es.udc.fic.corpuslab.modules.project.dtos.SaveProjectAnnotationStepResponseDto;
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
    private final EntityManager entityManager;

    public ProjectAnnotationServiceImpl(
            ProjectParticipantRepository projectParticipantRepository,
            DatasetItemRepository datasetItemRepository,
            AnnotationRepository annotationRepository,
            AuthApiService authApiService,
            NotificationService notificationService,
            EntityManager entityManager) {
        this.projectParticipantRepository = projectParticipantRepository;
        this.datasetItemRepository = datasetItemRepository;
        this.annotationRepository = annotationRepository;
        this.authApiService = authApiService;
        this.notificationService = notificationService;
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
                .findByProjectIdOrderByRoleAscUserLastNameAscUserFirstNameAsc(projectId);
        List<DatasetItem> datasetItems = datasetItemRepository.findByProjectIdOrderByItemIndexAsc(projectId);

        DatasetItem targetItem = datasetItems.stream()
                .filter(datasetItem -> datasetItem.getId().equals(request.datasetItemId()))
                .findFirst()
                .orElseThrow(() -> new InvalidProjectDatasetException("Dataset item does not belong to this project"));

        Map<Long, Map<Long, Map<Integer, Object>>> annotationLookupBeforeSave = buildAnnotationLookup(projectId);
        ProjectAnnotationUtils.ProjectProgressSnapshot progressBeforeSave = ProjectAnnotationUtils
                .buildProjectProgressSnapshot(participants, datasetItems, annotationLookupBeforeSave);
        int completionBeforeSave = progressBeforeSave.completionPercentageForUser(userInfo.userId());

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

        Map<Long, Map<Long, Map<Integer, Object>>> annotationLookupAfterSave = buildAnnotationLookup(projectId);
        ProjectAnnotationUtils.ProjectProgressSnapshot progressAfterSave = ProjectAnnotationUtils
                .buildProjectProgressSnapshot(participants, datasetItems, annotationLookupAfterSave);
        int completionAfterSave = progressAfterSave.completionPercentageForUser(userInfo.userId());

        if (completionBeforeSave < 100 && completionAfterSave == 100) {
            notifyProjectOwnersOnAnnotationCompletion(participant.getProject(), participants, userInfo);
        }

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
    @Transactional(readOnly = true)
    public ProjectAnnotationExportCsvDto exportAnnotationResultsCsv(String authenticatedEmail, Long projectId) {
        UserInfo requesterInfo = authApiService.findUserByEmail(authenticatedEmail);

        ProjectParticipant requesterParticipant = projectParticipantRepository
                .findByProjectIdAndUserId(projectId, requesterInfo.userId())
                .orElseThrow(() -> new ProjectNotFoundException(projectId));

        if (requesterParticipant.getRole() != ProjectParticipantRole.CREATOR) {
            throw new AccessDeniedException("Only project creators can export annotation results");
        }

        Project project = requesterParticipant.getProject();
        List<DatasetItem> datasetItems = datasetItemRepository.findByProjectIdOrderByItemIndexAsc(projectId);
        Map<Long, Map<Long, Map<Integer, Object>>> annotationLookup = buildAnnotationLookup(projectId);

        List<ProjectAnnotationUtils.AnnotatorExportColumn> annotatorColumns = buildAnnotatorExportColumns(projectId);

        LinkedHashSet<String> csvColumns = new LinkedHashSet<>();
        List<ProjectAnnotationUtils.ExportStepRow> exportRows = new ArrayList<>();

        for (DatasetItem datasetItem : datasetItems) {
            ProjectDatasetUtils.DatasetStepDefinition stepDefinition = ProjectDatasetUtils
                    .resolveStepDefinition(datasetItem);
            int totalSteps = stepDefinition.totalSteps();

            for (int stepIndex = 0; stepIndex < totalSteps; stepIndex++) {
                Map<String, String> rowValues = stepDefinition.rowValuesForStep(stepIndex);
                if (rowValues != null && !rowValues.isEmpty()) {
                    csvColumns.addAll(rowValues.keySet());
                }

                Map<Long, Object> annotationsByUser = new LinkedHashMap<>();
                for (ProjectAnnotationUtils.AnnotatorExportColumn annotatorColumn : annotatorColumns) {
                    annotationsByUser.put(
                            annotatorColumn.userId(),
                            findStepAnnotation(annotationLookup, datasetItem.getId(), annotatorColumn.userId(),
                                    stepIndex));
                }

                exportRows.add(new ProjectAnnotationUtils.ExportStepRow(
                        datasetItem,
                        stepIndex,
                        stepDefinition.previewForStep(stepIndex),
                        rowValues == null ? Map.of() : rowValues,
                        annotationsByUser));
            }
        }

        boolean isCsvDataset = datasetItems.stream().anyMatch(ProjectDatasetUtils::isCsvDatasetItem);

        // Determine which annotator columns have at least one non-empty value
        List<ProjectAnnotationUtils.AnnotatorExportColumn> nonEmptyAnnotatorColumns = new ArrayList<>();
        for (ProjectAnnotationUtils.AnnotatorExportColumn annotatorColumn : annotatorColumns) {
            boolean hasAnnotation = false;
            boolean hasComment = false;

            for (ProjectAnnotationUtils.ExportStepRow row : exportRows) {
                Object annotation = row.annotationsByUser().get(annotatorColumn.userId());
                if (!hasAnnotation && !extractAnnotationValue(annotation).isEmpty()) {
                    hasAnnotation = true;
                }
                if (!hasComment && !extractCommentValue(annotation).isEmpty()) {
                    hasComment = true;
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

        List<String> headers = new ArrayList<>();
        if (!isCsvDataset) {
            headers.add("dataset_item_index");
            headers.add("source_name");
        }
        headers.addAll(csvColumns);

        for (ProjectAnnotationUtils.AnnotatorExportColumn annotatorColumn : nonEmptyAnnotatorColumns) {
            if (annotatorColumn.hasAnnotation()) {
                headers.add(annotatorColumn.annotationHeader());
            }
            if (annotatorColumn.hasComment()) {
                headers.add(annotatorColumn.commentHeader());
            }
        }

        StringBuilder csvBuilder = new StringBuilder();
        appendCsvLine(csvBuilder, headers);

        for (ProjectAnnotationUtils.ExportStepRow row : exportRows) {
            DatasetItem datasetItem = row.datasetItem();
            List<String> values = new ArrayList<>();

            if (!isCsvDataset) {
                values.add(String.valueOf(datasetItem.getItemIndex()));
                values.add(ProjectDatasetUtils
                        .valueAsString(datasetItem.getContent().get(ProjectConstants.CONTENT_KEY_FILE_NAME)));
            }

            for (String csvColumn : csvColumns) {
                values.add(row.rowValues().getOrDefault(csvColumn, ""));
            }

            for (ProjectAnnotationUtils.AnnotatorExportColumn annotatorColumn : nonEmptyAnnotatorColumns) {
                Object annotation = row.annotationsByUser().get(annotatorColumn.userId());
                if (annotatorColumn.hasAnnotation()) {
                    values.add(extractAnnotationValue(annotation));
                }
                if (annotatorColumn.hasComment()) {
                    values.add(extractCommentValue(annotation));
                }
            }

            appendCsvLine(csvBuilder, values);
        }

        return new ProjectAnnotationExportCsvDto(
                buildAnnotationExportFileName(project),
                csvBuilder.toString().getBytes(StandardCharsets.UTF_8));
    }

    private ProjectAnnotationWorkspaceDto buildAnnotationWorkspace(
            Project project, Long annotationUserId, int offset, int limit) {
        Long projectId = project.getId();

        List<DatasetItem> datasetItems = datasetItemRepository.findByProjectIdOrderByItemIndexAsc(projectId);
        List<ProjectParticipant> participants = projectParticipantRepository
                .findByProjectIdOrderByRoleAscUserLastNameAscUserFirstNameAsc(projectId);
        Map<Long, Map<Long, Map<Integer, Object>>> annotationLookup = buildAnnotationLookup(projectId);

        ProjectAnnotationUtils.ProjectProgressSnapshot progressSnapshot = ProjectAnnotationUtils
                .buildProjectProgressSnapshot(participants, datasetItems, annotationLookup);

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
                findFirstPendingStepIndex(datasetItems, annotationUserId, annotationLookup),
                buildAnnotationSteps(datasetItems, annotationUserId, sanitizedOffset, sanitizedLimit,
                        annotationLookup));
    }

    private int sanitizeAnnotationStepsLimit(int limit) {
        if (limit <= 0) {
            return ProjectConstants.DEFAULT_ANNOTATION_STEPS_LIMIT;
        }
        return Math.min(limit, ProjectConstants.MAX_ANNOTATION_STEPS_LIMIT);
    }

    private List<ProjectAnnotationStepDto> buildAnnotationSteps(
            List<DatasetItem> datasetItems, Long userId, int offset, int limit,
            Map<Long, Map<Long, Map<Integer, Object>>> annotationLookup) {
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

                Object annotation = findStepAnnotation(annotationLookup, datasetItem.getId(), userId, stepIndex);

                steps.add(new ProjectAnnotationStepDto(
                        datasetItem.getId(), datasetItem.getItemIndex(), stepIndex, totalSteps,
                        ProjectDatasetUtils.valueAsString(
                                datasetItem.getContent().get(ProjectConstants.CONTENT_KEY_FILE_NAME)),
                        ProjectDatasetUtils.valueAsString(
                                datasetItem.getContent().get(ProjectConstants.CONTENT_KEY_MIME_TYPE)),
                        definition.previewForStep(stepIndex),
                        definition.rowValuesForStep(stepIndex),
                        ProjectAnnotationUtils.hasAnnotationPayload(annotation),
                        annotation));
            }
        }
        return steps;
    }

    private int findFirstPendingStepIndex(List<DatasetItem> datasetItems, Long userId,
            Map<Long, Map<Long, Map<Integer, Object>>> annotationLookup) {
        int globalStepIndex = 1;
        for (DatasetItem datasetItem : datasetItems) {
            ProjectDatasetUtils.DatasetStepDefinition stepDefinition = ProjectDatasetUtils
                    .resolveStepDefinition(datasetItem);
            int totalStepsForItem = stepDefinition.totalSteps();
            for (int stepIndex = 0; stepIndex < totalStepsForItem; stepIndex++) {
                Object annotation = findStepAnnotation(annotationLookup, datasetItem.getId(), userId, stepIndex);
                if (!ProjectAnnotationUtils.hasAnnotationPayload(annotation)) {
                    return globalStepIndex;
                }
                globalStepIndex++;
            }
        }
        return globalStepIndex > 1 ? 1 : 0;
    }

    private Map<Long, Map<Long, Map<Integer, Object>>> buildAnnotationLookup(Long projectId) {
        List<Annotation> annotations = annotationRepository.findByDatasetItemProjectId(projectId);
        Map<Long, Map<Long, Map<Integer, Object>>> annotationLookup = new LinkedHashMap<>();
        for (Annotation annotation : annotations) {
            if (annotation.getDatasetItem() == null || annotation.getUser() == null) continue;
            Long datasetItemId = annotation.getDatasetItem().getId();
            Long userId = annotation.getUser().getId();
            Integer stepIndex = annotation.getStepIndex();
            if (datasetItemId == null || userId == null || stepIndex == null) continue;
            annotationLookup
                    .computeIfAbsent(datasetItemId, ignored -> new LinkedHashMap<>())
                    .computeIfAbsent(userId, ignored -> new LinkedHashMap<>())
                    .put(stepIndex, annotation.getPayload());
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

    private Object findStepAnnotation(Map<Long, Map<Long, Map<Integer, Object>>> annotationLookup,
            Long datasetItemId, Long userId, int stepIndex) {
        Map<Long, Map<Integer, Object>> userAnnotationsByDataset = annotationLookup.get(datasetItemId);
        if (userAnnotationsByDataset == null) return null;
        Map<Integer, Object> stepsByUser = userAnnotationsByDataset.get(userId);
        if (stepsByUser == null) return null;
        Object annotationPayload = stepsByUser.get(stepIndex);
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

    private void appendCsvLine(StringBuilder csvBuilder, List<String> values) {
        for (int index = 0; index < values.size(); index++) {
            if (index > 0) csvBuilder.append(',');
            csvBuilder.append(escapeCsvValue(values.get(index)));
        }
        csvBuilder.append('\n');
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
        for (ProjectParticipant pp : projectParticipantRepository
                .findByProjectIdOrderByRoleAscUserLastNameAscUserFirstNameAsc(projectId)) {
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
}