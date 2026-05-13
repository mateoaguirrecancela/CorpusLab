package es.udc.fic.corpuslab.modules.project.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import es.udc.fic.corpuslab.modules.project.entities.Project;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    long countByResearchGroupId(Long researchGroupId);

    long countByResearchGroupIdAndArchivedFalse(Long researchGroupId);

    List<Project> findByResearchGroupId(Long researchGroupId);

    @Modifying
    @Query("""
            delete from Project p
            where p.researchGroup.id = :researchGroupId
            """)
    int deleteByResearchGroupId(@Param("researchGroupId") Long researchGroupId);

    Optional<Project> findByIdAndResearchGroupId(Long projectId, Long researchGroupId);
}
