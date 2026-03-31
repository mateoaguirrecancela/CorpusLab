package es.udc.fic.corpuslab.modules.researchgroup.controllers;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
