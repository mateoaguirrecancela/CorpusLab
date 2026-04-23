package es.udc.fic.corpuslab.modules.project.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import es.udc.fic.corpuslab.modules.project.entities.Project;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    long countByResearchGroupId(Long researchGroupId);

    List<Project> findByResearchGroupId(Long researchGroupId);

    Optional<Project> findByIdAndResearchGroupId(Long projectId, Long researchGroupId);
}
