package es.udc.fic.corpuslab.modules.project.shared.repositories;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import es.udc.fic.corpuslab.modules.project.progress.UserAnnotationCountDto;
import es.udc.fic.corpuslab.modules.project.shared.entities.Annotation;

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
                        select new es.udc.fic.corpuslab.modules.project.progress.UserAnnotationCountDto(
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

        long countByUserIdAndUpdatedAtGreaterThanEqualAndUpdatedAtLessThan(
                        Long userId,
                        Instant startInclusive,
                        Instant endExclusive);

        @Query("""
                        select count(a)
                        from Annotation a
                        join a.datasetItem di
                        join di.project p
                        join ProjectParticipant pp on pp.project = p and pp.user.id = :userId
                        where a.user.id = :userId
                          and a.warning = true
                          and p.archived = false
                        """)
        long countActiveWarningsByUserId(@Param("userId") Long userId);

        @Query("""
                        select count(a)
                        from Annotation a
                        join a.datasetItem di
                        join di.project p
                        join ProjectParticipant pp on pp.project = p and pp.user.id = :userId
                        where a.user.id = :userId
                          and a.warning = true
                          and p.archived = false
                          and a.warningMarkedAt >= :startInclusive
                        """)
        long countActiveWarningsByUserIdAndWarningMarkedAtGreaterThanEqual(
                        @Param("userId") Long userId,
                        @Param("startInclusive") Instant startInclusive);

        @Query(value = """
                        select
                            p.research_group_id as researchGroupId,
                            rg.name as researchGroupName,
                            cast(a.updated_at as date) as isoDate,
                            count(*) as annotationCount
                        from annotations a
                        join dataset_items d on d.id = a.dataset_item_id
                        join projects p on p.id = d.project_id
                        join research_groups rg on rg.id = p.research_group_id
                        join project_participants pp on pp.project_id = p.id and pp.user_id = :userId
                        where a.updated_at >= :startInclusive
                          and a.updated_at < :endExclusive
                        group by p.research_group_id, rg.name, cast(a.updated_at as date)
                        order by rg.name asc, cast(a.updated_at as date) asc
                        """, nativeQuery = true)
        List<AnnotationTrendRow> findDailyAnnotationTrendsByUserId(
                        @Param("userId") Long userId,
                        @Param("startInclusive") Instant startInclusive,
                        @Param("endExclusive") Instant endExclusive);

        Optional<Annotation> findByDatasetItemIdAndUserIdAndStepIndex(Long datasetItemId, Long userId,
                        Integer stepIndex);

        Optional<Annotation> findByDatasetItemIdAndDatasetItemProjectIdAndUserIdAndStepIndex(
                        Long datasetItemId,
                        Long projectId,
                        Long userId,
                        Integer stepIndex);

        long deleteByDatasetItemIdAndUserIdAndStepIndex(Long datasetItemId, Long userId, Integer stepIndex);

        long deleteByDatasetItemProjectId(Long projectId);

        @Modifying
        @Query("""
                        delete from Annotation a
                        where a.datasetItem.project.researchGroup.id = :researchGroupId
                        """)
        int deleteByResearchGroupId(@Param("researchGroupId") Long researchGroupId);

        long deleteByDatasetItemProjectIdAndUserIdIn(Long projectId, Collection<Long> userIds);

        interface AnnotationTrendRow {

                Long getResearchGroupId();

                String getResearchGroupName();

                LocalDate getIsoDate();

                Long getAnnotationCount();
        }
}
