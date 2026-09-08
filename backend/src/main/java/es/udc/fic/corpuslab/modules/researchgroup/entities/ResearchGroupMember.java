package es.udc.fic.corpuslab.modules.researchgroup.entities;

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

import java.time.Instant;

import es.udc.fic.corpuslab.modules.auth.entities.User;
import es.udc.fic.corpuslab.modules.researchgroup.enums.ResearchGroupMemberRole;

@Entity
@Table(name = "research_group_members", indexes = {
        @Index(name = "idx_research_group_members_group_deleted", columnList = "research_group_id, deleted_at"),
        @Index(name = "idx_research_group_members_user_deleted", columnList = "user_id, deleted_at")
})
public class ResearchGroupMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ResearchGroupMemberRole role;

    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "research_group_id", nullable = false)
    private ResearchGroup researchGroup;

    @PrePersist
    void onCreate() {
        this.joinedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public ResearchGroupMemberRole getRole() {
        return role;
    }

    public void setRole(ResearchGroupMemberRole role) {
        this.role = role;
    }

    public Instant getJoinedAt() {
        return joinedAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(Instant deletedAt) {
        this.deletedAt = deletedAt;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public ResearchGroup getResearchGroup() {
        return researchGroup;
    }

    public void setResearchGroup(ResearchGroup researchGroup) {
        this.researchGroup = researchGroup;
    }
}
