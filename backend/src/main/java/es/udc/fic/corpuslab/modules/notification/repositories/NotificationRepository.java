package es.udc.fic.corpuslab.modules.notification.repositories;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import es.udc.fic.corpuslab.modules.notification.entities.Notification;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByRecipientUserIdOrderByCreatedAtDesc(Long recipientUserId, Pageable pageable);

    long countByRecipientUserIdAndReadAtIsNull(Long recipientUserId);

    Optional<Notification> findByIdAndRecipientUserId(Long id, Long recipientUserId);

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
}
