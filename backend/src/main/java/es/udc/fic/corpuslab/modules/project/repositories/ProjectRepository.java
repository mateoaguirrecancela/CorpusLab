package es.udc.fic.corpuslab.modules.project.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import es.udc.fic.corpuslab.modules.project.entities.Project;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    long countByResearchGroupId(Long researchGroupId);
}
