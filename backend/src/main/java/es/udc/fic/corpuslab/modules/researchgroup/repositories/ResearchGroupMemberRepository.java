package es.udc.fic.corpuslab.modules.researchgroup.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import es.udc.fic.corpuslab.modules.researchgroup.dtos.ResearchGroupMemberDto;
import es.udc.fic.corpuslab.modules.researchgroup.dtos.ResearchGroupSummaryDto;
import es.udc.fic.corpuslab.modules.researchgroup.entities.ResearchGroupMember;

public interface ResearchGroupMemberRepository extends JpaRepository<ResearchGroupMember, Long> {

    @Query("""
            SELECT new es.udc.fic.corpuslab.modules.researchgroup.dtos.ResearchGroupMemberDto(
                u.id,
                u.firstName,
                u.lastName,
                u.email,
                m.role
            )
            FROM ResearchGroupMember m
            JOIN m.user u
            WHERE m.researchGroup.id = :groupId AND m.deletedAt IS NULL
            ORDER BY u.lastName ASC, u.firstName ASC
            """)
    List<ResearchGroupMemberDto> findMembersByGroupId(@Param("groupId") Long groupId);

    @Query("""
            SELECT new es.udc.fic.corpuslab.modules.researchgroup.dtos.ResearchGroupSummaryDto(
                g.id,
                g.name,
                g.description,
                m.role,
                (SELECT COUNT(m2) FROM ResearchGroupMember m2
                 WHERE m2.researchGroup = g AND m2.deletedAt IS NULL),
                g.createdAt
            )
            FROM ResearchGroupMember m
            JOIN m.researchGroup g
            WHERE m.user.id = :userId AND m.deletedAt IS NULL
            ORDER BY g.name ASC
            """)
    List<ResearchGroupSummaryDto> findGroupSummariesByUserId(@Param("userId") Long userId);
}
