package es.udc.fic.corpuslab.modules.project.repositories;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import es.udc.fic.corpuslab.modules.project.dtos.UserAnnotationCountDto;
import es.udc.fic.corpuslab.modules.project.entities.Annotation;

public interface AnnotationRepository extends JpaRepository<Annotation, Long> {

    @Query("""
            select a from Annotation a
            join fetch a.datasetItem di
            join fetch a.user
            where di.project.id = :projectId
            """)
    List<Annotation> findByProjectIdWithDatasetItemAndUser(@Param("projectId") Long projectId);

    @Query("""
            select a from Annotation a
            join fetch a.datasetItem di
            where di.project.id = :projectId
              and a.user.id = :userId
            """)
    List<Annotation> findByProjectIdAndUserIdWithDatasetItem(
            @Param("projectId") Long projectId,
            @Param("userId") Long userId);

    @Query("""
            select new es.udc.fic.corpuslab.modules.project.dtos.UserAnnotationCountDto(
                a.user.id,
                count(a)
            )
            from Annotation a
            join a.datasetItem di
            where di.project.id = :projectId
            group by a.user.id
            """)
    List<UserAnnotationCountDto> countCompletedStepsByUser(@Param("projectId") Long projectId);

    long countByDatasetItemProjectIdAndUserId(Long projectId, Long userId);

    long countByDatasetItemProjectId(Long projectId);

    Optional<Annotation> findByDatasetItemIdAndUserIdAndStepIndex(Long datasetItemId, Long userId, Integer stepIndex);

    long deleteByDatasetItemIdAndUserIdAndStepIndex(Long datasetItemId, Long userId, Integer stepIndex);

    long deleteByDatasetItemProjectId(Long projectId);

    @Modifying
    @Query("""
            delete from Annotation a
            where a.datasetItem.project.researchGroup.id = :researchGroupId
            """)
    int deleteByResearchGroupId(@Param("researchGroupId") Long researchGroupId);

    long deleteByDatasetItemProjectIdAndUserIdIn(Long projectId, Collection<Long> userIds);
}
