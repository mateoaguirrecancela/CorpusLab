package es.udc.fic.corpuslab.modules.researchgroup.services;

import java.util.List;

import es.udc.fic.corpuslab.modules.researchgroup.dtos.CreateResearchGroupRequestDto;
import es.udc.fic.corpuslab.modules.researchgroup.dtos.ResearchGroupInvitationDto;
import es.udc.fic.corpuslab.modules.researchgroup.dtos.ResearchGroupDetailDto;
import es.udc.fic.corpuslab.modules.researchgroup.dtos.ResearchGroupMemberDto;
import es.udc.fic.corpuslab.modules.researchgroup.dtos.ResearchGroupSummaryDto;
import es.udc.fic.corpuslab.modules.researchgroup.dtos.UpdateResearchGroupRequestDto;
import es.udc.fic.corpuslab.modules.researchgroup.enums.ResearchGroupMemberRole;

public interface ResearchGroupService {

    List<ResearchGroupSummaryDto> findMyResearchGroups(String authenticatedEmail);

    ResearchGroupSummaryDto createResearchGroup(String authenticatedEmail, CreateResearchGroupRequestDto request);

    ResearchGroupDetailDto getResearchGroupDetail(String authenticatedEmail, Long groupId);

    ResearchGroupDetailDto updateResearchGroup(
            String authenticatedEmail,
            Long groupId,
            UpdateResearchGroupRequestDto request);

    void deleteResearchGroup(String authenticatedEmail, Long groupId);

    ResearchGroupInvitationDto inviteResearcherByEmail(
            String authenticatedEmail,
            Long groupId,
            String invitedEmail,
            ResearchGroupMemberRole role);

    List<ResearchGroupInvitationDto> findMyPendingInvitations(String authenticatedEmail);

    ResearchGroupSummaryDto acceptMyInvitation(String authenticatedEmail, Long invitationId);

    void declineMyInvitation(String authenticatedEmail, Long invitationId);

    ResearchGroupMemberDto updateMemberRole(
            String authenticatedEmail,
            Long groupId,
            Long memberUserId,
            ResearchGroupMemberRole role);

    void removeMember(String authenticatedEmail, Long groupId, Long memberUserId);

    ResearchGroupSummaryDto joinResearchGroupByCode(String authenticatedEmail, String invitationCode);
}
