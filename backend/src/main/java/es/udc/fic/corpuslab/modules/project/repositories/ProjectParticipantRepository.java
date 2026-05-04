package es.udc.fic.corpuslab.modules.project.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import es.udc.fic.corpuslab.modules.project.entities.ProjectParticipant;
import es.udc.fic.corpuslab.modules.project.enums.ProjectParticipantRole;

public interface ProjectParticipantRepository extends JpaRepository<ProjectParticipant, Long> {

    @Transactional
    long deleteByProjectId(Long projectId);

    @Transactional
    long deleteByProjectIdAndRole(Long projectId, ProjectParticipantRole role);

    long countByProjectResearchGroupIdAndUserId(Long researchGroupId, Long userId);

    List<ProjectParticipant> findByProjectResearchGroupIdAndUserIdOrderByProjectCreatedAtDesc(
            Long researchGroupId,
            Long userId);

    Slice<ProjectParticipant> findByProjectResearchGroupIdAndUserIdAndProjectArchivedFalseOrderByProjectCreatedAtDesc(
            Long researchGroupId,
            Long userId,
            Pageable pageable);

    Slice<ProjectParticipant> findByProjectResearchGroupIdAndUserIdAndProjectArchivedTrueOrderByProjectCreatedAtDesc(
            Long researchGroupId,
            Long userId,
            Pageable pageable);

    List<ProjectParticipant> findByProjectIdOrderByRoleAscUserLastNameAscUserFirstNameAsc(Long projectId);

    List<ProjectParticipant> findByUserIdOrderByProjectCreatedAtDesc(Long userId);

    Slice<ProjectParticipant> findByUserIdAndProjectArchivedFalseOrderByProjectCreatedAtDesc(
            Long userId,
            Pageable pageable);

    Slice<ProjectParticipant> findByUserIdAndProjectArchivedTrueOrderByProjectCreatedAtDesc(
            Long userId,
            Pageable pageable);

    Optional<ProjectParticipant> findByProjectIdAndUserId(Long projectId, Long userId);
}
