package es.udc.fic.corpuslab.modules.project.fixtures;

import es.udc.fic.corpuslab.modules.project.shared.entities.Project;
import es.udc.fic.corpuslab.modules.project.shared.enums.ProjectType;
import es.udc.fic.corpuslab.modules.researchgroup.entities.ResearchGroup;

public class ProjectTestBuilder {

    private final Project project;

    private ProjectTestBuilder() {
        project = new Project();
        project.setName("Test Project");
        project.setDescription("Test Description");
        project.setProjectType(ProjectType.TEXT_CLASSIFICATION_SIMPLE);
        project.setSetupCompleted(false);
    }

    public static ProjectTestBuilder validProject() {
        return new ProjectTestBuilder();
    }

    public ProjectTestBuilder withName(String name) {
        project.setName(name);
        return this;
    }

    public ProjectTestBuilder withDescription(String description) {
        project.setDescription(description);
        return this;
    }

    public ProjectTestBuilder withResearchGroup(ResearchGroup group) {
        project.setResearchGroup(group);
        return this;
    }

    public ProjectTestBuilder withProjectType(ProjectType type) {
        project.setProjectType(type);
        return this;
    }

    public ProjectTestBuilder withSetupCompleted(boolean completed) {
        project.setSetupCompleted(completed);
        return this;
    }

    public Project build() {
        return project;
    }
}
