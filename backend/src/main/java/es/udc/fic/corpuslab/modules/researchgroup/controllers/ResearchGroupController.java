package es.udc.fic.corpuslab.modules.researchgroup.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import es.udc.fic.corpuslab.modules.researchgroup.dtos.CreateResearchGroupRequestDto;
import es.udc.fic.corpuslab.modules.researchgroup.dtos.ResearchGroupDetailDto;
import es.udc.fic.corpuslab.modules.researchgroup.dtos.ResearchGroupSummaryDto;
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
}
