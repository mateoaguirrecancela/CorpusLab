package es.udc.fic.corpuslab.modules.researchgroup.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.security.SecureRandom;
import java.util.Locale;

@Entity
@Table(name = "research_groups")
public class ResearchGroup {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String ALPHANUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 256)
    private String name;

    @Column(length = 2048)
    private String description;

    @Column(name = "invitation_code", nullable = false, unique = true, length = 64, updatable = false)
    private String invitationCode;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
        if (this.invitationCode == null || this.invitationCode.isBlank()) {
            this.invitationCode = generateInvitationCode(this.name);
        }
    }

    private static String generateInvitationCode(String groupName) {
        String normalizedPrefix = groupName == null
                ? "GRUPO"
                : groupName.toUpperCase(Locale.ROOT)
                        .replaceAll("[^A-Z0-9]+", "-")
                        .replaceAll("-{2,}", "-")
                        .replaceAll("^-|-$", "");

        if (normalizedPrefix.isBlank()) {
            normalizedPrefix = "GRUPO";
        }

        if (normalizedPrefix.length() > 24) {
            normalizedPrefix = normalizedPrefix.substring(0, 24);
        }

        StringBuilder suffix = new StringBuilder(12);
        for (int i = 0; i < 12; i++) {
            int index = RANDOM.nextInt(ALPHANUMERIC.length());
            suffix.append(ALPHANUMERIC.charAt(index));
        }

        return normalizedPrefix + "-" + suffix;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getInvitationCode() {
        return invitationCode;
    }
}
