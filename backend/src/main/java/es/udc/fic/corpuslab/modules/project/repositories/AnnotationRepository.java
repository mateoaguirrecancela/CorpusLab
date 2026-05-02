package es.udc.fic.corpuslab.modules.project.repositories;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import es.udc.fic.corpuslab.modules.project.entities.Annotation;

public interface AnnotationRepository extends JpaRepository<Annotation, Long> {

    List<Annotation> findByDatasetItemProjectId(Long projectId);

    Optional<Annotation> findByDatasetItemIdAndUserIdAndStepIndex(Long datasetItemId, Long userId, Integer stepIndex);

    long deleteByDatasetItemIdAndUserIdAndStepIndex(Long datasetItemId, Long userId, Integer stepIndex);

    long deleteByDatasetItemProjectId(Long projectId);

    long deleteByDatasetItemProjectIdAndUserIdIn(Long projectId, Collection<Long> userIds);
}
