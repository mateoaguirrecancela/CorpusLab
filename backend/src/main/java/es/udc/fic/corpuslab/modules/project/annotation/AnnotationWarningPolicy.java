package es.udc.fic.corpuslab.modules.project.annotation;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import es.udc.fic.corpuslab.modules.project.shared.entities.ProjectParticipant;
import es.udc.fic.corpuslab.modules.project.shared.enums.ProjectParticipantRole;

@Component
public class AnnotationWarningPolicy {

    public void requireCreator(ProjectParticipant requesterParticipant) {
        if (requesterParticipant.getRole() != ProjectParticipantRole.CREATOR) {
            throw new AccessDeniedException("Only project creators can toggle annotation warnings");
        }
    }
}
