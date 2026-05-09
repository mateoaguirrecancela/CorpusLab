package es.udc.fic.corpuslab.modules.project.services;

import java.util.List;

import es.udc.fic.corpuslab.modules.project.dtos.AssignProjectParticipantsRequestDto;
import es.udc.fic.corpuslab.modules.project.dtos.ProjectParticipantAssignmentDto;

public interface ProjectParticipantService {

    void assignParticipants(String authenticatedEmail, Long researchGroupId, Long projectId,
            AssignProjectParticipantsRequestDto request);

    void assignParticipants(String authenticatedEmail, Long researchGroupId, Long projectId,
            List<Long> participantUserIds);

    void replaceProjectParticipants(Long projectId, Long requesterUserId, Long researchGroupId,
            List<Long> participantUserIds);

    void replaceProjectParticipantAssignments(Long projectId, Long requesterUserId, Long researchGroupId,
            List<ProjectParticipantAssignmentDto> participantAssignments);

    void removeParticipantFromAllGroupProjects(Long researchGroupId, Long userId);

}
