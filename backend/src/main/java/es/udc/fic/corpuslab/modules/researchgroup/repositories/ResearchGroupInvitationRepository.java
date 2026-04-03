package es.udc.fic.corpuslab.modules.researchgroup.repositories;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import es.udc.fic.corpuslab.modules.researchgroup.dtos.ResearchGroupInvitationDto;
import es.udc.fic.corpuslab.modules.researchgroup.entities.ResearchGroupInvitation;
import es.udc.fic.corpuslab.modules.researchgroup.enums.ResearchGroupInvitationStatus;

public interface ResearchGroupInvitationRepository extends JpaRepository<ResearchGroupInvitation, Long> {

        boolean existsByToken(String token);

        @Query("""
                        SELECT COUNT(i) > 0
                        FROM ResearchGroupInvitation i
                        WHERE i.researchGroup.id = :researchGroupId
                          AND UPPER(i.invitedEmail) = UPPER(:invitedEmail)
                          AND i.status = :status
                          AND i.expiresAt > :now
                        """)
        boolean existsActivePendingInvitation(
                        @Param("researchGroupId") Long researchGroupId,
                        @Param("invitedEmail") String invitedEmail,
                        @Param("status") ResearchGroupInvitationStatus status,
                        @Param("now") Instant now);

        @Query("""
                                SELECT new es.udc.fic.corpuslab.modules.researchgroup.dtos.ResearchGroupInvitationDto(
                                    i.id,
                                    g.id,
                                    g.name,
                                    i.invitedEmail,
                                    CONCAT(inviter.firstName, ' ', inviter.lastName),
                        i.role,
                                    i.status,
                        i.createdAt,
                        i.expiresAt
                                )
                                FROM ResearchGroupInvitation i
                                JOIN i.researchGroup g
                                JOIN i.inviterUser inviter
                                WHERE UPPER(i.invitedEmail) = UPPER(:email)
                                  AND i.status = es.udc.fic.corpuslab.modules.researchgroup.enums.ResearchGroupInvitationStatus.PENDING
                            AND i.expiresAt > :now
                                ORDER BY i.createdAt DESC
                                """)
        List<ResearchGroupInvitationDto> findPendingInvitationsByInvitedEmail(
                        @Param("email") String email,
                        @Param("now") Instant now);

        @Query("""
                        SELECT i
                        FROM ResearchGroupInvitation i
                        WHERE i.id = :invitationId
                          AND UPPER(i.invitedEmail) = UPPER(:email)
                          AND i.status = es.udc.fic.corpuslab.modules.researchgroup.enums.ResearchGroupInvitationStatus.PENDING
                          AND i.expiresAt > :now
                        """)
        Optional<ResearchGroupInvitation> findActivePendingInvitationByIdAndInvitedEmail(
                        @Param("invitationId") Long invitationId,
                        @Param("email") String email,
                        @Param("now") Instant now);

        @Query("""
                        SELECT i
                        FROM ResearchGroupInvitation i
                        WHERE i.researchGroup.id = :researchGroupId
                          AND UPPER(i.invitedEmail) = UPPER(:email)
                          AND i.status = es.udc.fic.corpuslab.modules.researchgroup.enums.ResearchGroupInvitationStatus.PENDING
                          AND i.expiresAt > :now
                        """)
        List<ResearchGroupInvitation> findActivePendingInvitationsByGroupIdAndInvitedEmail(
                        @Param("researchGroupId") Long researchGroupId,
                        @Param("email") String email,
                        @Param("now") Instant now);
}
