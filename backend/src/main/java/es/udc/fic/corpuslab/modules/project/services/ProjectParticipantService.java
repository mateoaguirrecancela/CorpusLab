package es.udc.fic.corpuslab.modules.project.services;

import java.util.List;

public interface ProjectParticipantService {

    void assignParticipants(String authenticatedEmail, Long researchGroupId, Long projectId,
            List<Long> participantUserIds);

    void replaceProjectParticipants(Long projectId, Long requesterUserId, Long researchGroupId,
            List<Long> participantUserIds);

    void removeParticipantFromAllGroupProjects(Long researchGroupId, Long userId);

}
