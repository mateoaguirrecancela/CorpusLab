package es.udc.fic.corpuslab.modules.project.progress;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.udc.fic.corpuslab.modules.project.dataset.dtos.DatasetItemSummaryProjection;
import es.udc.fic.corpuslab.modules.project.progress.UserAnnotationCountDto;
import es.udc.fic.corpuslab.modules.project.shared.entities.Annotation;
import es.udc.fic.corpuslab.modules.project.shared.entities.DatasetItem;
import es.udc.fic.corpuslab.modules.project.shared.entities.ProjectParticipant;
import es.udc.fic.corpuslab.modules.project.shared.repositories.AnnotationRepository;
import es.udc.fic.corpuslab.modules.project.shared.repositories.DatasetItemRepository;
import es.udc.fic.corpuslab.modules.project.shared.repositories.ProjectParticipantRepository;
import es.udc.fic.corpuslab.modules.project.shared.utils.ProjectAnnotationUtils;
import es.udc.fic.corpuslab.modules.project.shared.utils.ProjectDatasetUtils;

@Service
public class ProjectProgressCalculator {

    private final ProjectParticipantRepository projectParticipantRepository;
    private final DatasetItemRepository datasetItemRepository;
    private final AnnotationRepository annotationRepository;

    public ProjectProgressCalculator(
            ProjectParticipantRepository projectParticipantRepository,
            DatasetItemRepository datasetItemRepository,
            AnnotationRepository annotationRepository) {
        this.projectParticipantRepository = projectParticipantRepository;
        this.datasetItemRepository = datasetItemRepository;
        this.annotationRepository = annotationRepository;
    }

    @Transactional(readOnly = true)
    public int calculateProjectCompletionPercentage(Long projectId) {
        return ProjectAnnotationUtils.buildProjectProgressSnapshot(
                projectParticipantRepository.findByProjectIdWithUserAndProject(projectId),
                datasetItemRepository.findByProjectIdOrderByItemIndexAsc(projectId),
                buildAnnotationLookup(projectId))
                .projectCompletionPercentage();
    }

    @Transactional(readOnly = true)
    public ProjectProgressSnapshot buildProjectProgressSnapshot(
            Long projectId,
            List<ProjectParticipant> participants,
            List<DatasetItemSummaryProjection> datasetItemSummaries) {
        return buildProjectProgressSnapshot(
                participants,
                resolveTotalStepsFromSummaries(datasetItemSummaries),
                buildCompletedStepCountMap(projectId));
    }

    @Transactional(readOnly = true)
    public ProjectProgressSnapshot buildProjectProgressSnapshotForDatasetItems(
            Long projectId,
            List<ProjectParticipant> participants,
            List<DatasetItem> datasetItems) {
        return buildProjectProgressSnapshot(
                participants,
                countTotalSteps(datasetItems),
                buildCompletedStepCountMap(projectId));
    }

    @Transactional(readOnly = true)
    public Map<Long, Long> buildCompletedStepCountMap(Long projectId) {
        List<UserAnnotationCountDto> counts = annotationRepository.countCompletedStepsByUser(projectId);
        Map<Long, Long> result = new LinkedHashMap<>();
        for (UserAnnotationCountDto count : counts) {
            if (count.userId() != null) {
                result.put(count.userId(), count.completedSteps());
            }
        }
        return result;
    }

    public ProjectProgressSnapshot buildProjectProgressSnapshot(
            List<ProjectParticipant> participants,
            long totalSteps,
            Map<Long, Long> completedStepCounts) {
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

        return new ProjectProgressSnapshot(
                totalSteps,
                ProjectAnnotationUtils.toPercentage(totalCompletedSteps, totalPossibleSteps),
                completedStepsByUser,
                completionPercentageByUser);
    }

    private long resolveTotalStepsFromSummaries(List<DatasetItemSummaryProjection> datasetItemSummaries) {
        return datasetItemSummaries.stream()
                .map(DatasetItemSummaryProjection::getStepCount)
                .mapToLong(Long::longValue)
                .sum();
    }

    private long countTotalSteps(List<DatasetItem> datasetItems) {
        long totalSteps = 0L;
        for (DatasetItem datasetItem : datasetItems) {
            totalSteps += ProjectDatasetUtils.resolveStepDefinition(datasetItem).totalSteps();
        }
        return totalSteps;
    }

    private Map<Long, Map<Long, Map<Integer, Object>>> buildAnnotationLookup(Long projectId) {
        List<Annotation> annotations = annotationRepository.findByProjectIdWithDatasetItemAndUser(projectId);
        Map<Long, Map<Long, Map<Integer, Object>>> annotationLookup = new LinkedHashMap<>();
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
                    .put(stepIndex, annotation.getPayload());
        }

        return annotationLookup;
    }
}
