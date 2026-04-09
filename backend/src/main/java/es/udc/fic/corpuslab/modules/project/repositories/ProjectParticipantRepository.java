package es.udc.fic.corpuslab.modules.project.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import es.udc.fic.corpuslab.modules.project.entities.ProjectParticipant;
import es.udc.fic.corpuslab.modules.project.enums.ProjectParticipantRole;

public interface ProjectParticipantRepository extends JpaRepository<ProjectParticipant, Long> {

    @Transactional
    long deleteByProjectIdAndRole(Long projectId, ProjectParticipantRole role);

    List<ProjectParticipant> findByProjectResearchGroupIdAndUserIdOrderByProjectCreatedAtDesc(
            Long researchGroupId,
            Long userId);

    List<ProjectParticipant> findByUserIdOrderByProjectCreatedAtDesc(Long userId);

    Optional<ProjectParticipant> findByProjectIdAndUserId(Long projectId, Long userId);
}
