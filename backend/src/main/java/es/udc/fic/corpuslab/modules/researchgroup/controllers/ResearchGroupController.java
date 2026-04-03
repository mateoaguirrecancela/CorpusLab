package es.udc.fic.corpuslab.modules.researchgroup.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import es.udc.fic.corpuslab.modules.researchgroup.dtos.CreateResearchGroupRequestDto;
import es.udc.fic.corpuslab.modules.researchgroup.dtos.CreateResearchGroupInvitationRequestDto;
import es.udc.fic.corpuslab.modules.researchgroup.dtos.JoinResearchGroupByCodeRequestDto;
import es.udc.fic.corpuslab.modules.researchgroup.dtos.ResearchGroupInvitationDto;
import es.udc.fic.corpuslab.modules.researchgroup.dtos.ResearchGroupDetailDto;
import es.udc.fic.corpuslab.modules.researchgroup.dtos.ResearchGroupMemberDto;
import es.udc.fic.corpuslab.modules.researchgroup.dtos.ResearchGroupSummaryDto;
import es.udc.fic.corpuslab.modules.researchgroup.dtos.UpdateResearchGroupMemberRoleRequestDto;
import es.udc.fic.corpuslab.modules.researchgroup.dtos.UpdateResearchGroupRequestDto;
import es.udc.fic.corpuslab.modules.researchgroup.services.ResearchGroupService;

@RestController
@RequestMapping("/api/research-groups")
public class ResearchGroupController {

    private final ResearchGroupService researchGroupService;

    public ResearchGroupController(ResearchGroupService researchGroupService) {
        this.researchGroupService = researchGroupService;
    }

    @GetMapping
    public List<ResearchGroupSummaryDto> myGroups(Authentication authentication) {
        return researchGroupService.findMyResearchGroups(authentication.getName());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResearchGroupSummaryDto createGroup(
            Authentication authentication,
            @Valid @RequestBody CreateResearchGroupRequestDto request) {
        return researchGroupService.createResearchGroup(authentication.getName(), request);
    }

    @GetMapping("/{id}")
    public ResearchGroupDetailDto getGroupDetail(
            Authentication authentication,
            @PathVariable Long id) {
        return researchGroupService.getResearchGroupDetail(authentication.getName(), id);
    }

    @PutMapping("/{id}")
    public ResearchGroupDetailDto updateGroup(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody UpdateResearchGroupRequestDto request) {
        return researchGroupService.updateResearchGroup(authentication.getName(), id, request);
    }

    @PostMapping("/{id}/members/{memberUserId}/role")
    public ResearchGroupMemberDto updateMemberRole(
            Authentication authentication,
            @PathVariable Long id,
            @PathVariable Long memberUserId,
            @Valid @RequestBody UpdateResearchGroupMemberRoleRequestDto request) {
        return researchGroupService.updateMemberRole(
                authentication.getName(),
                id,
                memberUserId,
                request.role());
    }

    @PostMapping("/{id}/members/{memberUserId}/remove")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(
            Authentication authentication,
            @PathVariable Long id,
            @PathVariable Long memberUserId) {
        researchGroupService.removeMember(authentication.getName(), id, memberUserId);
    }

    @PostMapping("/{id}/invitations")
    @ResponseStatus(HttpStatus.CREATED)
    public ResearchGroupInvitationDto inviteResearcher(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody CreateResearchGroupInvitationRequestDto request) {
        return researchGroupService.inviteResearcherByEmail(
                authentication.getName(),
                id,
                request.email(),
                request.role(),
                request.expiresAt());
    }

    @GetMapping("/my-invitations")
    public List<ResearchGroupInvitationDto> myInvitations(Authentication authentication) {
        return researchGroupService.findMyPendingInvitations(authentication.getName());
    }

    @PostMapping("/my-invitations/{invitationId}/accept")
    public ResearchGroupSummaryDto acceptInvitation(
            Authentication authentication,
            @PathVariable Long invitationId) {
        return researchGroupService.acceptMyInvitation(authentication.getName(), invitationId);
    }

    @PostMapping("/my-invitations/{invitationId}/decline")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void declineInvitation(
            Authentication authentication,
            @PathVariable Long invitationId) {
        researchGroupService.declineMyInvitation(authentication.getName(), invitationId);
    }

    @PostMapping("/join-by-code")
    public ResearchGroupSummaryDto joinByCode(
            Authentication authentication,
            @Valid @RequestBody JoinResearchGroupByCodeRequestDto request) {
        return researchGroupService.joinResearchGroupByCode(authentication.getName(), request.code());
    }
}
