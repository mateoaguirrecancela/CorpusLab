package es.udc.fic.corpuslab.modules.notification.entities;

import java.time.Instant;

import es.udc.fic.corpuslab.modules.auth.entities.User;
import es.udc.fic.corpuslab.modules.notification.enums.NotificationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "notifications", indexes = {
        @Index(name = "idx_notifications_recipient_created_at", columnList = "recipient_user_id, created_at"),
        @Index(name = "idx_notifications_recipient_read_at", columnList = "recipient_user_id, read_at")
})
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "recipient_user_id", nullable = false)
    private User recipientUser;

    @ManyToOne
    @JoinColumn(name = "actor_user_id")
    private User actorUser;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 64)
    private NotificationType type;

    @Column(name = "research_group_id")
    private Long researchGroupId;

    @Column(name = "research_group_name", length = 255)
    private String researchGroupName;

    @Column(name = "invitation_id")
    private Long invitationId;

    @Column(name = "project_id")
    private Long projectId;

    @Column(name = "project_name", length = 255)
    private String projectName;

    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public User getRecipientUser() {
        return recipientUser;
    }

    public void setRecipientUser(User recipientUser) {
        this.recipientUser = recipientUser;
    }

    public User getActorUser() {
        return actorUser;
    }

    public void setActorUser(User actorUser) {
        this.actorUser = actorUser;
    }

    public NotificationType getType() {
        return type;
    }

    public void setType(NotificationType type) {
        this.type = type;
    }

    public Long getResearchGroupId() {
        return researchGroupId;
    }

    public void setResearchGroupId(Long researchGroupId) {
        this.researchGroupId = researchGroupId;
    }

    public String getResearchGroupName() {
        return researchGroupName;
    }

    public void setResearchGroupName(String researchGroupName) {
        this.researchGroupName = researchGroupName;
    }

    public Long getInvitationId() {
        return invitationId;
    }

    public void setInvitationId(Long invitationId) {
        this.invitationId = invitationId;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public Instant getReadAt() {
        return readAt;
    }

    public void setReadAt(Instant readAt) {
        this.readAt = readAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
