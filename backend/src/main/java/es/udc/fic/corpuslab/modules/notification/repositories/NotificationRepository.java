package es.udc.fic.corpuslab.modules.notification.repositories;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import es.udc.fic.corpuslab.modules.notification.dtos.NotificationDto;
import es.udc.fic.corpuslab.modules.notification.entities.Notification;
import es.udc.fic.corpuslab.modules.notification.enums.NotificationType;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

        @Query("""
                        select new es.udc.fic.corpuslab.modules.notification.dtos.NotificationDto(
                            n.id,
                            n.type,
                            case when n.readAt is not null then true else false end,
                            n.createdAt,
                            case
                                when actor.id is null then null
                                else trim(concat(actor.firstName, ' ', actor.lastName))
                            end,
                            n.researchGroupId,
                            n.researchGroupName,
                            n.invitationId,
                            n.projectId,
                            n.projectName
                        )
                        from Notification n
                        left join n.actorUser actor
                        where n.recipientUser.id = :recipientUserId
                        order by n.createdAt desc
                        """)
        List<NotificationDto> findDtosByRecipientUserIdOrderByCreatedAtDesc(
                        @Param("recipientUserId") Long recipientUserId,
                        Pageable pageable);

        long countByRecipientUserIdAndReadAtIsNull(Long recipientUserId);

        Optional<Notification> findByIdAndRecipientUserId(Long id, Long recipientUserId);

        boolean existsByRecipientUserIdAndActorUserIdAndTypeAndProjectId(
                        Long recipientUserId,
                        Long actorUserId,
                        NotificationType type,
                        Long projectId);

        @Query("""
                        select distinct n.recipientUser.id
                        from Notification n
                        where n.projectId = :projectId
                        """)
        List<Long> findRecipientUserIdsByProjectId(@Param("projectId") Long projectId);

        @Query("""
                        select distinct n.recipientUser.id
                        from Notification n
                        where n.researchGroupId = :researchGroupId
                        """)
        List<Long> findRecipientUserIdsByResearchGroupId(@Param("researchGroupId") Long researchGroupId);

        @Modifying
        @Query("""
                        UPDATE Notification n
                        SET n.readAt = :readAt
                        WHERE n.recipientUser.id = :recipientUserId
                          AND n.readAt IS NULL
                        """)
        int markAllAsReadByRecipientUserId(
                        @Param("recipientUserId") Long recipientUserId,
                        @Param("readAt") Instant readAt);

        @Transactional
        long deleteByProjectId(Long projectId);

        @Transactional
        long deleteByResearchGroupId(Long researchGroupId);
}
