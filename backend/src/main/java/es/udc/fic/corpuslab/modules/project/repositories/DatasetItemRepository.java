package es.udc.fic.corpuslab.modules.project.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import es.udc.fic.corpuslab.modules.project.entities.DatasetItem;

public interface DatasetItemRepository extends JpaRepository<DatasetItem, Long> {

    long countByProjectId(Long projectId);

    List<DatasetItem> findByProjectIdOrderByItemIndexAsc(Long projectId);
}
