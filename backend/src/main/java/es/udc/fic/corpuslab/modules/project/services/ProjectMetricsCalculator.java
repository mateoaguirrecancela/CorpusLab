package es.udc.fic.corpuslab.modules.project.services;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.udc.fic.corpuslab.modules.project.dtos.ProjectMetricsDto;
import es.udc.fic.corpuslab.modules.project.entities.Annotation;
import es.udc.fic.corpuslab.modules.project.entities.DatasetItem;
import es.udc.fic.corpuslab.modules.project.entities.Project;
import es.udc.fic.corpuslab.modules.project.entities.ProjectParticipant;
import es.udc.fic.corpuslab.modules.project.enums.IaaResultStatus;
import es.udc.fic.corpuslab.modules.project.enums.MetricType;
import es.udc.fic.corpuslab.modules.project.enums.ProjectParticipantIaaGroup;
import es.udc.fic.corpuslab.modules.project.iaa.calculators.IaaMetricCalculator;
import es.udc.fic.corpuslab.modules.project.iaa.calculators.ner.SpanOverlapUnit;
import es.udc.fic.corpuslab.modules.project.iaa.context.AnnotationCalculationContext;
import es.udc.fic.corpuslab.modules.project.iaa.dtos.IaaResult;
import es.udc.fic.corpuslab.modules.project.iaa.factory.MetricsFactory;
import es.udc.fic.corpuslab.modules.project.iaa.transformers.xrr.XrrAnnotationDataTransformer;
import es.udc.fic.corpuslab.modules.project.repositories.AnnotationRepository;
import es.udc.fic.corpuslab.modules.project.repositories.DatasetItemRepository;
import es.udc.fic.corpuslab.modules.project.repositories.ProjectParticipantRepository;

@Service
public class ProjectMetricsCalculator {

    private static final String METADATA_KEY_ANNOTATION_TARGET_COLUMN = "annotationTargetColumn";

    private final ProjectParticipantRepository projectParticipantRepository;
    private final DatasetItemRepository datasetItemRepository;
    private final AnnotationRepository annotationRepository;
    private final MetricsFactory metricsFactory;

    public ProjectMetricsCalculator(
            ProjectParticipantRepository projectParticipantRepository,
            DatasetItemRepository datasetItemRepository,
            AnnotationRepository annotationRepository,
            MetricsFactory metricsFactory) {
        this.projectParticipantRepository = projectParticipantRepository;
        this.datasetItemRepository = datasetItemRepository;
        this.annotationRepository = annotationRepository;
        this.metricsFactory = metricsFactory;
    }

    @Transactional(readOnly = true)
    public ProjectMetricsDto calculate(Long projectId) {
        List<ProjectParticipant> annotators = projectParticipantRepository
                .findByProjectIdWithUserAndProject(projectId);
        if (annotators.isEmpty()) {
            return new ProjectMetricsDto(projectId, List.of());
        }

        Project project = annotators.get(0).getProject();
        List<DatasetItem> datasetItems = datasetItemRepository.findByProjectIdOrderByItemIndexAsc(projectId);
        List<Annotation> annotations = annotationRepository.findByProjectIdWithDatasetItemAndUser(projectId);

        AnnotationCalculationContext context = AnnotationCalculationContext.fromProject(
                project,
                annotations,
                datasetItems,
                annotators,
                buildMetadata(project, annotators));

        List<IaaResult> metrics = metricsFactory.supportedMetrics(project.getProjectType())
                .stream()
                .sorted(Comparator.comparingInt(MetricType::ordinal))
                .map(metricType -> calculateMetric(project, metricType, context))
                .toList();

        return new ProjectMetricsDto(project.getId(), metrics);
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

    private IaaResult calculateMetric(
            Project project,
            MetricType metricType,
            AnnotationCalculationContext context) {
        try {
            IaaMetricCalculator calculator = metricsFactory.getCalculator(project.getProjectType(), metricType);
            return calculator.calculate(context);
        } catch (RuntimeException ex) {
            return IaaResult.notCalculable(
                    metricType,
                    project.getProjectType(),
                    IaaResultStatus.UNDEFINED,
                    "Metric could not be calculated: " + ex.getMessage(),
                    context.annotators().size(),
                    context.datasetItems().size(),
                    0,
                    Map.of("errorType", ex.getClass().getSimpleName()));
        }
    }
}
