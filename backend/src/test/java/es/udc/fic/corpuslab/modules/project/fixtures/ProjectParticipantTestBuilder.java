package es.udc.fic.corpuslab.modules.project.fixtures;

import es.udc.fic.corpuslab.modules.auth.entities.User;
import es.udc.fic.corpuslab.modules.project.entities.Project;
import es.udc.fic.corpuslab.modules.project.entities.ProjectParticipant;
import es.udc.fic.corpuslab.modules.project.enums.ProjectParticipantRole;

public class ProjectParticipantTestBuilder {

    private final ProjectParticipant participant;

    private ProjectParticipantTestBuilder() {
        participant = new ProjectParticipant();
        participant.setRole(ProjectParticipantRole.PARTICIPANT);
    }

    public static ProjectParticipantTestBuilder validParticipant() {
        return new ProjectParticipantTestBuilder();
    }

    public ProjectParticipantTestBuilder withUser(User user) {
        participant.setUser(user);
        return this;
    }

    public ProjectParticipantTestBuilder withProject(Project project) {
        participant.setProject(project);
        return this;
    }

    public ProjectParticipantTestBuilder withRole(ProjectParticipantRole role) {
        participant.setRole(role);
        return this;
    }

    public ProjectParticipant build() {
        return participant;
    }
}
