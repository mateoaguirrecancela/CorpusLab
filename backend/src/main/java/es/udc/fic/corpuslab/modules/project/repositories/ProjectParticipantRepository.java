package es.udc.fic.corpuslab.modules.project.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import es.udc.fic.corpuslab.modules.project.entities.ProjectParticipant;
import es.udc.fic.corpuslab.modules.project.enums.ProjectParticipantRole;

public interface ProjectParticipantRepository extends JpaRepository<ProjectParticipant, Long> {

    @Transactional
    long deleteByProjectId(Long projectId);

    @Transactional
    long deleteByProjectIdAndRole(Long projectId, ProjectParticipantRole role);

    @Transactional
    @Modifying
    @Query("""
            delete from ProjectParticipant pp
            where pp.project.researchGroup.id = :researchGroupId
            """)
    int deleteByProjectResearchGroupId(@Param("researchGroupId") Long researchGroupId);

    long countByProjectResearchGroupIdAndUserId(Long researchGroupId, Long userId);

    long countByProjectIdAndRole(Long projectId, ProjectParticipantRole role);

    List<ProjectParticipant> findByProjectResearchGroupIdAndUserIdOrderByProjectCreatedAtDesc(
            Long researchGroupId,
            Long userId);

    @EntityGraph(attributePaths = { "project", "project.researchGroup" })
    Slice<ProjectParticipant> findByProjectResearchGroupIdAndUserIdAndProjectArchivedFalseOrderByProjectCreatedAtDesc(
            Long researchGroupId,
            Long userId,
            Pageable pageable);

    @EntityGraph(attributePaths = { "project", "project.researchGroup" })
    Slice<ProjectParticipant> findByProjectResearchGroupIdAndUserIdAndProjectArchivedTrueOrderByProjectCreatedAtDesc(
            Long researchGroupId,
            Long userId,
            Pageable pageable);

    List<ProjectParticipant> findByProjectIdOrderByRoleAscUserLastNameAscUserFirstNameAsc(Long projectId);

    @Query("""
            select pp from ProjectParticipant pp
            join fetch pp.user u
            join fetch pp.project p
            join fetch p.researchGroup
            where p.id = :projectId
            order by pp.role asc, u.lastName asc, u.firstName asc
            """)
    List<ProjectParticipant> findByProjectIdWithUserAndProject(@Param("projectId") Long projectId);

    List<ProjectParticipant> findByUserIdOrderByProjectCreatedAtDesc(Long userId);

    @EntityGraph(attributePaths = { "project", "project.researchGroup" })
    Slice<ProjectParticipant> findByUserIdAndProjectArchivedFalseOrderByProjectCreatedAtDesc(
            Long userId,
            Pageable pageable);

    @EntityGraph(attributePaths = { "project", "project.researchGroup" })
    Slice<ProjectParticipant> findByUserIdAndProjectArchivedTrueOrderByProjectCreatedAtDesc(
            Long userId,
            Pageable pageable);

    Optional<ProjectParticipant> findByProjectIdAndUserId(Long projectId, Long userId);
}
