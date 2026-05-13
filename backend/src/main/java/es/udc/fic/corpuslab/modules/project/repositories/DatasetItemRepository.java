package es.udc.fic.corpuslab.modules.project.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import es.udc.fic.corpuslab.modules.project.dtos.DatasetItemSummaryProjection;
import es.udc.fic.corpuslab.modules.project.entities.DatasetItem;

public interface DatasetItemRepository extends JpaRepository<DatasetItem, Long> {

    long countByProjectId(Long projectId);

    long deleteByProjectId(Long projectId);

    @Modifying
    @Query("""
            delete from DatasetItem d
            where d.project.researchGroup.id = :researchGroupId
            """)
    int deleteByProjectResearchGroupId(@Param("researchGroupId") Long researchGroupId);

    List<DatasetItem> findByProjectIdOrderByItemIndexAsc(Long projectId);

    @Query(value = """
            select
                d.id as id,
                d.item_index as itemIndex,
                coalesce(d.content ->> 'fileName', '') as fileName,
                coalesce(d.content ->> 'mimeType', '') as mimeType,
                coalesce(cast(nullif(d.content ->> 'sizeBytes', '') as bigint), 0) as sizeBytes,
                cast(nullif(d.content ->> 'stepCount', '') as bigint) as stepCount,
                d.created_at as createdAt
            from dataset_items d
            where d.project_id = :projectId
            order by d.item_index asc
            """, nativeQuery = true)
    List<DatasetItemSummaryProjection> findSummariesByProjectId(@Param("projectId") Long projectId);

    Optional<DatasetItem> findByIdAndProjectId(Long id, Long projectId);
}
