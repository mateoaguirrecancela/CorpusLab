package es.udc.fic.corpuslab.modules.project.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import es.udc.fic.corpuslab.modules.project.entities.ProjectParticipant;
import es.udc.fic.corpuslab.modules.project.enums.ProjectParticipantRole;

public interface ProjectParticipantRepository extends JpaRepository<ProjectParticipant, Long> {

    @Transactional
    long deleteByProjectIdAndRole(Long projectId, ProjectParticipantRole role);
}
