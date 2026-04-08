package es.udc.fic.corpuslab.modules.project.entities;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import es.udc.fic.corpuslab.modules.project.enums.ProjectType;
import jakarta.persistence.CascadeType;
import es.udc.fic.corpuslab.modules.researchgroup.entities.ResearchGroup;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "projects")
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "research_group_id", nullable = false)
    private ResearchGroup researchGroup;

    @Column(nullable = false, length = 256)
    private String name;

    @Column(length = 2048)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "project_type", nullable = false, length = 64)
    private ProjectType projectType;

    @Column(name = "setup_completed", nullable = false)
    private boolean setupCompleted;

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Label> labels = new ArrayList<>();

    @OneToOne(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private Guideline guideline;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
        if (this.projectType == null) {
            this.projectType = ProjectType.TEXT_CLASSIFICATION_SIMPLE;
        }
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

    public ProjectType getProjectType() {
        return projectType;
    }

    public void setProjectType(ProjectType projectType) {
        this.projectType = projectType;
    }

    public boolean isSetupCompleted() {
        return setupCompleted;
    }

    public void setSetupCompleted(boolean setupCompleted) {
        this.setupCompleted = setupCompleted;
    }

    public List<Label> getLabels() {
        return labels;
    }

    public Guideline getGuideline() {
        return guideline;
    }

    public void setGuideline(Guideline guideline) {
        this.guideline = guideline;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
