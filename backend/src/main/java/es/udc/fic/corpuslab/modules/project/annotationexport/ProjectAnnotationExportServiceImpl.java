package es.udc.fic.corpuslab.modules.project.annotationexport;

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

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.udc.fic.corpuslab.common.utils.StringUtils;
import es.udc.fic.corpuslab.modules.auth.api.AuthApiService;
import es.udc.fic.corpuslab.modules.auth.api.dtos.UserInfo;
import es.udc.fic.corpuslab.modules.project.shared.entities.Annotation;
import es.udc.fic.corpuslab.modules.project.shared.entities.DatasetItem;
import es.udc.fic.corpuslab.modules.project.shared.entities.Project;
import es.udc.fic.corpuslab.modules.project.shared.entities.ProjectParticipant;
import es.udc.fic.corpuslab.modules.project.shared.enums.ProjectParticipantRole;
import es.udc.fic.corpuslab.modules.project.shared.exceptions.ProjectNotFoundException;
import es.udc.fic.corpuslab.modules.project.shared.repositories.AnnotationRepository;
import es.udc.fic.corpuslab.modules.project.shared.repositories.DatasetItemRepository;
import es.udc.fic.corpuslab.modules.project.shared.repositories.ProjectParticipantRepository;
import es.udc.fic.corpuslab.modules.project.shared.utils.ProjectAnnotationUtils;
import es.udc.fic.corpuslab.modules.project.shared.utils.ProjectConstants;
import es.udc.fic.corpuslab.modules.project.shared.utils.ProjectDatasetUtils;

@Service
public class ProjectAnnotationExportServiceImpl implements ProjectAnnotationExportService {

    private final ProjectParticipantRepository projectParticipantRepository;
    private final DatasetItemRepository datasetItemRepository;
    private final AnnotationRepository annotationRepository;
    private final AuthApiService authApiService;

    public ProjectAnnotationExportServiceImpl(
            ProjectParticipantRepository projectParticipantRepository,
            DatasetItemRepository datasetItemRepository,
            AnnotationRepository annotationRepository,
            AuthApiService authApiService) {
        this.projectParticipantRepository = projectParticipantRepository;
        this.datasetItemRepository = datasetItemRepository;
        this.annotationRepository = annotationRepository;
        this.authApiService = authApiService;
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
        findExportableProject(authenticatedEmail, projectId);
        List<DatasetItem> datasetItems = datasetItemRepository.findByProjectIdOrderByItemIndexAsc(projectId);
        Map<Long, Map<Long, Map<Integer, Annotation>>> annotationLookup = buildAnnotationLookup(projectId);
        List<ProjectAnnotationUtils.AnnotatorExportColumn> annotatorColumns = buildAnnotatorExportColumns(projectId);

        LinkedHashSet<String> csvColumns = collectCsvColumns(datasetItems);
        List<ProjectAnnotationUtils.AnnotatorExportColumn> nonEmptyAnnotatorColumns = buildNonEmptyAnnotatorColumns(
                datasetItems,
                annotationLookup,
                annotatorColumns);

        return new CsvExportContext(
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

    private Map<Long, Map<Long, Map<Integer, Annotation>>> buildAnnotationLookup(Long projectId) {
        List<Annotation> annotations = annotationRepository.findByProjectIdWithDatasetItemAndUser(projectId);
        Map<Long, Map<Long, Map<Integer, Annotation>>> annotationLookup = new LinkedHashMap<>();
        for (Annotation annotation : annotations) {
            if (annotation.getDatasetItem() == null || annotation.getUser() == null) {
                continue;
            }
            Long datasetItemId = annotation.getDatasetItem().getId();
            Long userId = annotation.getUser().getId();
            Integer stepIndex = annotation.getStepIndex();
            if (datasetItemId == null || userId == null || stepIndex == null) {
                continue;
            }
            annotationLookup
                    .computeIfAbsent(datasetItemId, ignored -> new LinkedHashMap<>())
                    .computeIfAbsent(userId, ignored -> new LinkedHashMap<>())
                    .put(stepIndex, annotation);
        }
        return annotationLookup;
    }

    private Annotation findStepAnnotation(
            Map<Long, Map<Long, Map<Integer, Annotation>>> annotationLookup,
            Long datasetItemId,
            Long userId,
            int stepIndex) {
        Map<Long, Map<Integer, Annotation>> userAnnotationsByDataset = annotationLookup.get(datasetItemId);
        if (userAnnotationsByDataset == null) {
            return null;
        }
        Map<Integer, Annotation> stepsByUser = userAnnotationsByDataset.get(userId);
        if (stepsByUser == null) {
            return null;
        }
        return stepsByUser.get(stepIndex);
    }

    private void appendCsvLine(Writer writer, List<String> values) throws IOException {
        for (int index = 0; index < values.size(); index++) {
            if (index > 0) {
                writer.append(',');
            }
            writer.append(escapeCsvValue(values.get(index)));
        }
        writer.append('\n');
    }

    private String escapeCsvValue(String value) {
        if (value == null) {
            return "";
        }
        boolean mustBeQuoted = value.contains(",") || value.contains("\"")
                || value.contains("\n") || value.contains("\r");
        if (!mustBeQuoted) {
            return value;
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private String buildAnnotationExportFileName(Project project) {
        String projectName = StringUtils.trimToNull(project.getName());
        if (projectName == null) {
            return "project-" + project.getId() + "-annotations.csv";
        }
        String slugifiedName = projectName.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+)|(-+$)", "");
        if (slugifiedName.isBlank()) {
            return "project-" + project.getId() + "-annotations.csv";
        }
        return slugifiedName + "-annotations.csv";
    }

    private List<ProjectAnnotationUtils.AnnotatorExportColumn> buildAnnotatorExportColumns(Long projectId) {
        Map<Long, String> annotatorIdByUserId = new LinkedHashMap<>();
        for (ProjectParticipant projectParticipant : projectParticipantRepository.findByProjectIdWithUserAndProject(
                projectId)) {
            var annotator = projectParticipant.getUser();
            if (annotator == null || annotator.getId() == null) {
                continue;
            }
            annotatorIdByUserId.putIfAbsent(annotator.getId(), normalizeExportAnnotatorId(annotator.getId()));
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

    private String normalizeExportAnnotatorId(Long annotatorId) {
        return annotatorId.toString();
    }

    private String extractAnnotationValue(Object annotationPayload) {
        if (!ProjectAnnotationUtils.hasAnnotationPayload(annotationPayload)) {
            return "";
        }
        String normalizedBoolean = normalizeBooleanValue(annotationPayload);
        if (normalizedBoolean != null) {
            return normalizedBoolean;
        }
        if (annotationPayload instanceof String stringValue) {
            return stringValue.trim();
        }
        if (annotationPayload instanceof Number || annotationPayload instanceof Boolean) {
            return String.valueOf(annotationPayload);
        }
        if (annotationPayload instanceof List<?> listValue) {
            return normalizeListLikeValue(listValue);
        }
        if (!(annotationPayload instanceof Map<?, ?> annotationAsMap)) {
            return String.valueOf(annotationPayload);
        }

        Map<String, Object> annotationMap = ProjectAnnotationUtils.toMutableStringObjectMap(annotationAsMap);
        String labelValue = StringUtils.trimToNull(
                ProjectDatasetUtils.valueAsString(annotationMap.get(ProjectConstants.ANNOTATION_KEY_LABEL)));
        if (labelValue != null) {
            return labelValue;
        }
        String labelsValue = normalizeListLikeValue(annotationMap.get(ProjectConstants.ANNOTATION_KEY_LABELS));
        if (!labelsValue.isBlank()) {
            return labelsValue;
        }
        String textValue = StringUtils.trimToNull(
                ProjectDatasetUtils.valueAsString(annotationMap.get(ProjectConstants.ANNOTATION_KEY_TEXT)));
        if (textValue != null) {
            return textValue;
        }
        String entitiesValue = normalizeNerEntitiesForExport(
                annotationMap.get(ProjectConstants.NER_ANNOTATION_KEY_ENTITIES));
        if (!entitiesValue.isBlank()) {
            return entitiesValue;
        }
        String binaryValue = normalizeBooleanValue(
                annotationMap.get(ProjectConstants.ANNOTATION_KEY_BINARY_VALUE));
        if (binaryValue != null) {
            return binaryValue;
        }
        return annotationMap.toString();
    }

    private String extractCommentValue(Object annotationPayload) {
        if (!(annotationPayload instanceof Map<?, ?> annotationAsMap)) {
            return "";
        }
        Map<String, Object> annotationMap = ProjectAnnotationUtils.toMutableStringObjectMap(annotationAsMap);
        String notes = StringUtils.trimToNull(
                ProjectDatasetUtils.valueAsString(annotationMap.get(ProjectConstants.ANNOTATION_KEY_NOTES)));
        return notes == null ? "" : notes;
    }

    private String normalizeNerEntitiesForExport(Object rawEntities) {
        if (!(rawEntities instanceof List<?> entities)) {
            return "";
        }
        List<String> normalizedEntities = entities.stream().map(rawEntity -> {
            if (!(rawEntity instanceof Map<?, ?> rawEntityMap)) {
                return null;
            }
            Map<String, Object> entityMap = ProjectAnnotationUtils.toMutableStringObjectMap(rawEntityMap);
            String label = StringUtils.trimToNull(
                    ProjectDatasetUtils.valueAsString(entityMap.get(ProjectConstants.NER_ANNOTATION_KEY_LABEL)));
            String text = StringUtils.trimToNull(
                    ProjectDatasetUtils.valueAsString(entityMap.get(ProjectConstants.NER_ANNOTATION_KEY_TEXT)));
            if (label != null && text != null) {
                return label + ":" + text;
            }
            if (text != null) {
                return text;
            }
            return label;
        }).filter(value -> value != null && !value.isBlank()).distinct().toList();
        return String.join("|", normalizedEntities);
    }

    private String normalizeListLikeValue(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof String stringValue) {
            String normalized = stringValue.trim();
            return normalized.isEmpty() ? "" : normalized;
        }
        if (!(value instanceof List<?> listValue)) {
            return "";
        }
        List<String> normalized = listValue.stream()
                .filter(entry -> entry != null)
                .map(String::valueOf)
                .map(String::trim)
                .filter(entry -> !entry.isEmpty())
                .distinct()
                .toList();
        return String.join("|", normalized);
    }

    private String normalizeBooleanValue(Object value) {
        if (value instanceof Boolean booleanValue) {
            return String.valueOf(booleanValue);
        }
        if (value instanceof Number numberValue) {
            long longValue = numberValue.longValue();
            if (longValue == 0L || longValue == 1L) {
                return String.valueOf(longValue == 1L);
            }
            return null;
        }
        if (!(value instanceof String stringValue)) {
            return null;
        }
        String normalized = stringValue.trim().toLowerCase();
        if (normalized.equals("true") || normalized.equals("false")) {
            return normalized;
        }
        return null;
    }

    private record CsvExportContext(
            List<DatasetItem> datasetItems,
            Map<Long, Map<Long, Map<Integer, Annotation>>> annotationLookup,
            LinkedHashSet<String> csvColumns,
            List<ProjectAnnotationUtils.AnnotatorExportColumn> nonEmptyAnnotatorColumns,
            boolean isCsvDataset) {
    }
}
