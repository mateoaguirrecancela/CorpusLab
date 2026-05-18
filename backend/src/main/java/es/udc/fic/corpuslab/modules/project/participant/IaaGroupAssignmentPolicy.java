package es.udc.fic.corpuslab.modules.project.participant;

import org.springframework.stereotype.Component;

import es.udc.fic.corpuslab.modules.project.shared.enums.ProjectParticipantIaaGroup;

@Component
public class IaaGroupAssignmentPolicy {

    public ProjectParticipantIaaGroup defaultCreatorGroup(ProjectParticipantIaaGroup requestedGroup) {
        return requestedGroup == null ? ProjectParticipantIaaGroup.GROUP_A : requestedGroup;
    }
}
