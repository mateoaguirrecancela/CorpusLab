package es.udc.fic.corpuslab.modules.project.participant;

import java.util.List;

import es.udc.fic.corpuslab.modules.project.participant.dtos.AssignProjectParticipantsRequestDto;
import es.udc.fic.corpuslab.modules.project.participant.dtos.ProjectAssignmentContextDto;
import es.udc.fic.corpuslab.modules.project.participant.dtos.ProjectParticipantAssignmentDto;

public interface ProjectParticipantService {

    void assignParticipants(String authenticatedEmail, Long researchGroupId, Long projectId,
            AssignProjectParticipantsRequestDto request);

    void assignParticipants(String authenticatedEmail, Long researchGroupId, Long projectId,
            List<Long> participantUserIds);

    ProjectAssignmentContextDto getProjectAssignmentContext(String authenticatedEmail, Long researchGroupId);

    void replaceProjectParticipants(Long projectId, Long requesterUserId, Long researchGroupId,
            List<Long> participantUserIds);

    void replaceProjectParticipantAssignments(Long projectId, Long requesterUserId, Long researchGroupId,
            List<ProjectParticipantAssignmentDto> participantAssignments);

    void removeParticipantFromAllGroupProjects(Long researchGroupId, Long userId);

}
