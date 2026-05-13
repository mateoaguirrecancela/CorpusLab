package es.udc.fic.corpuslab.modules.project.services;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.udc.fic.corpuslab.modules.project.entities.Annotation;
import es.udc.fic.corpuslab.modules.project.repositories.AnnotationRepository;
import es.udc.fic.corpuslab.modules.project.repositories.DatasetItemRepository;
import es.udc.fic.corpuslab.modules.project.repositories.ProjectParticipantRepository;
import es.udc.fic.corpuslab.modules.project.utils.ProjectAnnotationUtils;

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
