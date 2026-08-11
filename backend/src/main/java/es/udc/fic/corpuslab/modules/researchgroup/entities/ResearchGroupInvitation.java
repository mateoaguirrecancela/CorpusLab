package es.udc.fic.corpuslab.modules.researchgroup.entities;

import java.time.Instant;

import es.udc.fic.corpuslab.modules.auth.entities.User;
import es.udc.fic.corpuslab.modules.researchgroup.enums.ResearchGroupInvitationStatus;
import es.udc.fic.corpuslab.modules.researchgroup.enums.ResearchGroupMemberRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "research_group_invitations")
public class ResearchGroupInvitation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "research_group_id", nullable = false)
    private ResearchGroup researchGroup;

    @ManyToOne(optional = false)
    @JoinColumn(name = "inviter_user_id", nullable = false)
    private User inviterUser;

    @ManyToOne
    @JoinColumn(name = "invited_user_id")
    private User invitedUser;

    @Column(name = "invited_email", nullable = false)
    private String invitedEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ResearchGroupMemberRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ResearchGroupInvitationStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public ResearchGroup getResearchGroup() {
        return researchGroup;
    }

    public void setResearchGroup(ResearchGroup researchGroup) {
        this.researchGroup = researchGroup;
    }

    public User getInviterUser() {
        return inviterUser;
    }

    public void setInviterUser(User inviterUser) {
        this.inviterUser = inviterUser;
    }

    public User getInvitedUser() {
        return invitedUser;
    }

    public void setInvitedUser(User invitedUser) {
        this.invitedUser = invitedUser;
    }

    public String getInvitedEmail() {
        return invitedEmail;
    }

    public void setInvitedEmail(String invitedEmail) {
        this.invitedEmail = invitedEmail;
    }

    public ResearchGroupMemberRole getRole() {
        return role;
    }

    public void setRole(ResearchGroupMemberRole role) {
        this.role = role;
    }

    public ResearchGroupInvitationStatus getStatus() {
        return status;
    }

    public void setStatus(ResearchGroupInvitationStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }
}
