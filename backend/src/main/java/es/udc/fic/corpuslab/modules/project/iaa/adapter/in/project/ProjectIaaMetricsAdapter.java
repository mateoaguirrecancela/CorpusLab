package es.udc.fic.corpuslab.modules.project.iaa.adapter.in.project;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.udc.fic.corpuslab.common.utils.StringUtils;
import es.udc.fic.corpuslab.modules.project.metrics.dtos.ProjectMetricsDto;
import es.udc.fic.corpuslab.modules.project.shared.entities.Annotation;
import es.udc.fic.corpuslab.modules.project.shared.entities.DatasetItem;
import es.udc.fic.corpuslab.modules.project.shared.entities.Project;
import es.udc.fic.corpuslab.modules.project.shared.entities.ProjectParticipant;
import es.udc.fic.corpuslab.modules.project.shared.enums.ProjectParticipantIaaGroup;
import es.udc.fic.corpuslab.modules.project.iaa.application.port.in.CalculateIaaMetricsCommand;
import es.udc.fic.corpuslab.modules.project.iaa.application.port.in.CalculateIaaMetricsUseCase;
import es.udc.fic.corpuslab.modules.project.iaa.domain.calculators.ner.SpanOverlapUnit;
import es.udc.fic.corpuslab.modules.project.iaa.domain.model.IaaAnnotation;
import es.udc.fic.corpuslab.modules.project.iaa.domain.model.IaaAnnotator;
import es.udc.fic.corpuslab.modules.project.iaa.domain.model.IaaCalculationContext;
import es.udc.fic.corpuslab.modules.project.iaa.domain.model.IaaDatasetItem;
import es.udc.fic.corpuslab.modules.project.iaa.domain.model.IaaDatasetStep;
import es.udc.fic.corpuslab.modules.project.iaa.domain.transformers.xrr.XrrAnnotationDataTransformer;
import es.udc.fic.corpuslab.modules.project.shared.repositories.AnnotationRepository;
import es.udc.fic.corpuslab.modules.project.shared.repositories.DatasetItemRepository;
import es.udc.fic.corpuslab.modules.project.shared.repositories.ProjectParticipantRepository;
import es.udc.fic.corpuslab.modules.project.shared.utils.ProjectConstants;
import es.udc.fic.corpuslab.modules.project.shared.utils.ProjectDatasetUtils;

@Service
public class ProjectIaaMetricsAdapter {

    private static final String METADATA_KEY_ANNOTATION_TARGET_COLUMN = "annotationTargetColumn";

    private final ProjectParticipantRepository projectParticipantRepository;
    private final DatasetItemRepository datasetItemRepository;
    private final AnnotationRepository annotationRepository;
    private final CalculateIaaMetricsUseCase calculateIaaMetricsUseCase;

    public ProjectIaaMetricsAdapter(
            ProjectParticipantRepository projectParticipantRepository,
            DatasetItemRepository datasetItemRepository,
            AnnotationRepository annotationRepository,
            CalculateIaaMetricsUseCase calculateIaaMetricsUseCase) {
        this.projectParticipantRepository = projectParticipantRepository;
        this.datasetItemRepository = datasetItemRepository;
        this.annotationRepository = annotationRepository;
        this.calculateIaaMetricsUseCase = calculateIaaMetricsUseCase;
    }

    @Transactional(readOnly = true)
    public ProjectMetricsDto calculate(Long projectId) {
        List<ProjectParticipant> participants = projectParticipantRepository
                .findByProjectIdWithUserAndProject(projectId);
        if (participants.isEmpty()) {
            return new ProjectMetricsDto(projectId, List.of());
        }

        Project project = participants.get(0).getProject();
        List<DatasetItem> datasetItems = datasetItemRepository.findByProjectIdOrderByItemIndexAsc(projectId);
        List<Annotation> annotations = annotationRepository.findByProjectIdWithDatasetItemAndUser(projectId);

        IaaCalculationContext context = new IaaCalculationContext(
                project.getId(),
                project.getProjectType(),
                annotations.stream().map(this::toIaaAnnotation).toList(),
                datasetItems.stream()
                        .map(datasetItem -> toIaaDatasetItem(datasetItem, project.getAnnotationTargetColumn()))
                        .toList(),
                participants.stream().map(this::toIaaAnnotator).toList(),
                buildMetadata(project, participants));

        return new ProjectMetricsDto(
                project.getId(),
                calculateIaaMetricsUseCase.calculate(new CalculateIaaMetricsCommand(context)).metrics());
    }

    private IaaAnnotation toIaaAnnotation(Annotation annotation) {
        return new IaaAnnotation(
                annotation.getDatasetItem() == null ? null : annotation.getDatasetItem().getId(),
                annotation.getUser() == null ? null : annotation.getUser().getId(),
                annotation.getStepIndex(),
                annotation.getPayload());
    }

    private IaaAnnotator toIaaAnnotator(ProjectParticipant participant) {
        return new IaaAnnotator(participant.getUser() == null ? null : participant.getUser().getId());
    }

    private IaaDatasetItem toIaaDatasetItem(DatasetItem datasetItem, String annotationTargetColumn) {
        ProjectDatasetUtils.DatasetStepDefinition definition = ProjectDatasetUtils.resolveStepDefinition(datasetItem);
        List<IaaDatasetStep> steps = java.util.stream.IntStream.range(0, definition.totalSteps())
                .mapToObj(stepIndex -> new IaaDatasetStep(
                        stepIndex,
                        definition.previewForStep(stepIndex),
                        definition.rowValuesForStep(stepIndex),
                        resolveSourceText(datasetItem, definition, stepIndex, annotationTargetColumn)))
                .toList();

        return new IaaDatasetItem(
                datasetItem.getId(),
                datasetItem.getItemIndex(),
                steps,
                ProjectDatasetUtils.isCsvDatasetItem(datasetItem));
    }

    private String resolveSourceText(
            DatasetItem datasetItem,
            ProjectDatasetUtils.DatasetStepDefinition definition,
            int stepIndex,
            String annotationTargetColumn) {
        Map<String, String> rowValues = definition.rowValuesForStep(stepIndex);
        String normalizedTargetColumn = StringUtils.trimToNull(annotationTargetColumn);
        if (rowValues != null && normalizedTargetColumn != null) {
            for (Map.Entry<String, String> entry : rowValues.entrySet()) {
                if (entry.getKey() != null && entry.getKey().trim().equalsIgnoreCase(normalizedTargetColumn)) {
                    return entry.getValue() == null ? "" : entry.getValue();
                }
            }
        }

        String decodedText = decodeTextDatasetItem(datasetItem);
        if (decodedText != null) {
            return decodedText;
        }

        return definition.previewForStep(stepIndex);
    }

    private String decodeTextDatasetItem(DatasetItem datasetItem) {
        if (datasetItem.getContent() == null) {
            return null;
        }

        String mimeType = ProjectDatasetUtils.valueAsString(
                datasetItem.getContent().get(ProjectConstants.CONTENT_KEY_MIME_TYPE)).toLowerCase();
        String fileName = ProjectDatasetUtils.valueAsString(
                datasetItem.getContent().get(ProjectConstants.CONTENT_KEY_FILE_NAME)).toLowerCase();
        if (!mimeType.startsWith("text/")
                && !mimeType.contains("json")
                && !fileName.endsWith(".txt")
                && !fileName.endsWith(".json")) {
            return null;
        }

        String base64 = ProjectDatasetUtils.valueAsString(
                datasetItem.getContent().get(ProjectConstants.CONTENT_KEY_BASE64));
        if (base64.isBlank()) {
            return null;
        }

        try {
            return new String(ProjectDatasetUtils.decodeStoredBase64(base64), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private Map<String, Object> buildMetadata(Project project, List<ProjectParticipant> annotators) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        if (project.getAnnotationTargetColumn() != null && !project.getAnnotationTargetColumn().isBlank()) {
            metadata.put(METADATA_KEY_ANNOTATION_TARGET_COLUMN, project.getAnnotationTargetColumn());
        }
        putXrrGroups(metadata, annotators);
        metadata.putIfAbsent(SpanOverlapUnit.METADATA_KEY, SpanOverlapUnit.CHARACTER.name());
        return metadata;
    }

    private void putXrrGroups(Map<String, Object> metadata, List<ProjectParticipant> annotators) {
        List<Long> groupAAnnotatorIds = annotatorIdsForGroup(annotators, ProjectParticipantIaaGroup::isGroupA);
        List<Long> groupBAnnotatorIds = annotatorIdsForGroup(annotators, ProjectParticipantIaaGroup::isGroupB);
        if (groupAAnnotatorIds.isEmpty() && groupBAnnotatorIds.isEmpty()) {
            return;
        }

        metadata.put(XrrAnnotationDataTransformer.METADATA_KEY_GROUP_X_ANNOTATOR_IDS, groupAAnnotatorIds);
        metadata.put(XrrAnnotationDataTransformer.METADATA_KEY_GROUP_Y_ANNOTATOR_IDS, groupBAnnotatorIds);
    }

    private List<Long> annotatorIdsForGroup(
            List<ProjectParticipant> annotators,
            java.util.function.Predicate<ProjectParticipantIaaGroup> groupPredicate) {
        return annotators.stream()
                .filter(participant -> participant.getIaaGroup() != null
                        && groupPredicate.test(participant.getIaaGroup()))
                .map(ProjectParticipant::getUser)
                .filter(user -> user != null && user.getId() != null)
                .map(user -> user.getId())
                .toList();
    }
}
