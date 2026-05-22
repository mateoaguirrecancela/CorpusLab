package es.udc.fic.corpuslab.modules.researchgroup.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import es.udc.fic.corpuslab.modules.researchgroup.dtos.ResearchGroupMemberDto;
import es.udc.fic.corpuslab.modules.researchgroup.dtos.ResearchGroupSummaryDto;
import es.udc.fic.corpuslab.modules.researchgroup.entities.ResearchGroupMember;

public interface ResearchGroupMemberRepository extends JpaRepository<ResearchGroupMember, Long> {

    long deleteByResearchGroupId(Long researchGroupId);

    List<ResearchGroupMember> findByResearchGroupId(Long researchGroupId);

    long countByResearchGroupIdAndDeletedAtIsNull(Long researchGroupId);

    @Query("""
            SELECT m
            FROM ResearchGroupMember m
            WHERE m.researchGroup.id = :groupId
              AND m.user.id = :userId
              AND m.deletedAt IS NULL
            """)
    Optional<ResearchGroupMember> findActiveMemberByGroupIdAndUserId(
            @Param("groupId") Long groupId,
            @Param("userId") Long userId);

    @Query("""
            SELECT COUNT(m) > 0
            FROM ResearchGroupMember m
            JOIN m.user u
            WHERE m.researchGroup.id = :groupId
              AND m.deletedAt IS NULL
              AND LOWER(u.email) = LOWER(:email)
            """)
    boolean existsActiveMemberByGroupIdAndEmail(
            @Param("groupId") Long groupId,
            @Param("email") String email);

    @Query("""
            SELECT u.id
            FROM ResearchGroupMember m
            JOIN m.user u
            WHERE m.researchGroup.id = :groupId
              AND m.deletedAt IS NULL
            """)
    List<Long> findActiveMemberUserIdsByGroupId(@Param("groupId") Long groupId);

    @Query("""
            SELECT u.id
            FROM ResearchGroupMember m
            JOIN m.user u
            WHERE m.researchGroup.id = :groupId
              AND m.deletedAt IS NULL
              AND m.role IN (
                es.udc.fic.corpuslab.modules.researchgroup.enums.ResearchGroupMemberRole.OWNER,
                es.udc.fic.corpuslab.modules.researchgroup.enums.ResearchGroupMemberRole.ADMIN
              )
            """)
    List<Long> findActiveOwnerAndAdminUserIdsByGroupId(@Param("groupId") Long groupId);

    @Query("""
            SELECT new es.udc.fic.corpuslab.modules.researchgroup.dtos.ResearchGroupMemberDto(
                u.id,
                u.firstName,
                u.lastName,
                u.email,
                m.role,
                COUNT(pp)
            )
            FROM ResearchGroupMember m
            JOIN m.user u
            LEFT JOIN ProjectParticipant pp
              ON pp.user = u
             AND pp.project.researchGroup.id = :groupId
             AND pp.project.archived = false
            WHERE m.researchGroup.id = :groupId
              AND m.user.id = :userId
              AND m.deletedAt IS NULL
            GROUP BY u.id, u.firstName, u.lastName, u.email, m.role
            """)
    Optional<ResearchGroupMemberDto> findMemberDtoByGroupIdAndUserId(
            @Param("groupId") Long groupId,
            @Param("userId") Long userId);

    @Query("""
            SELECT new es.udc.fic.corpuslab.modules.researchgroup.dtos.ResearchGroupMemberDto(
                u.id,
                u.firstName,
                u.lastName,
                u.email,
                m.role,
                COUNT(pp)
            )
            FROM ResearchGroupMember m
            JOIN m.user u
            LEFT JOIN ProjectParticipant pp
              ON pp.user = u
             AND pp.project.researchGroup.id = :groupId
             AND pp.project.archived = false
            WHERE m.researchGroup.id = :groupId AND m.deletedAt IS NULL
            GROUP BY u.id, u.firstName, u.lastName, u.email, m.role
            ORDER BY u.lastName ASC, u.firstName ASC
            """)
    List<ResearchGroupMemberDto> findMembersByGroupId(@Param("groupId") Long groupId);

    @Query("""
            SELECT new es.udc.fic.corpuslab.modules.researchgroup.dtos.ResearchGroupSummaryDto(
                g.id,
                g.name,
                g.description,
                m.role,
                COUNT(m2),
                g.createdAt
            )
            FROM ResearchGroupMember m
            JOIN m.researchGroup g
            LEFT JOIN ResearchGroupMember m2
              ON m2.researchGroup = g
             AND m2.deletedAt IS NULL
            WHERE m.user.id = :userId AND m.deletedAt IS NULL
            GROUP BY g.id, g.name, g.description, m.role, g.createdAt
            ORDER BY g.name ASC
            """)
    List<ResearchGroupSummaryDto> findGroupSummariesByUserId(@Param("userId") Long userId);
}
