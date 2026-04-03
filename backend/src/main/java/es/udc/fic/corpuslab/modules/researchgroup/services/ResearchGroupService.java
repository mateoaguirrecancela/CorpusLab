package es.udc.fic.corpuslab.modules.researchgroup.services;

import java.util.List;
import java.time.Instant;

import es.udc.fic.corpuslab.modules.researchgroup.dtos.CreateResearchGroupRequestDto;
import es.udc.fic.corpuslab.modules.researchgroup.dtos.ResearchGroupInvitationDto;
import es.udc.fic.corpuslab.modules.researchgroup.dtos.ResearchGroupDetailDto;
import es.udc.fic.corpuslab.modules.researchgroup.dtos.ResearchGroupSummaryDto;
import es.udc.fic.corpuslab.modules.researchgroup.enums.ResearchGroupMemberRole;

public interface ResearchGroupService {

    List<ResearchGroupSummaryDto> findMyResearchGroups(String authenticatedEmail);

    ResearchGroupSummaryDto createResearchGroup(String authenticatedEmail, CreateResearchGroupRequestDto request);

    ResearchGroupDetailDto getResearchGroupDetail(String authenticatedEmail, Long groupId);

    ResearchGroupInvitationDto inviteResearcherByEmail(
            String authenticatedEmail,
            Long groupId,
            String invitedEmail,
            ResearchGroupMemberRole role,
            Instant expiresAt);

    List<ResearchGroupInvitationDto> findMyPendingInvitations(String authenticatedEmail);

    ResearchGroupSummaryDto acceptMyInvitation(String authenticatedEmail, Long invitationId);

    void declineMyInvitation(String authenticatedEmail, Long invitationId);

    ResearchGroupSummaryDto joinResearchGroupByCode(String authenticatedEmail, String invitationCode);
}
