package es.udc.fic.corpuslab.modules.project.participant;

import java.util.Collection;
import java.util.Set;

import org.springframework.stereotype.Component;

import es.udc.fic.corpuslab.modules.project.shared.exceptions.InvalidProjectParticipantsException;

@Component
public class ProjectParticipantAssignmentPolicy {

    public void requireActiveResearchGroupMembers(Collection<Long> requestedUserIds, Set<Long> activeMemberIds) {
        boolean hasIdsOutsideGroup = requestedUserIds.stream().anyMatch(id -> !activeMemberIds.contains(id));
        if (hasIdsOutsideGroup) {
            throw new InvalidProjectParticipantsException(
                    "All selected investigators must be active members of the research group");
        }
    }
}
