package es.udc.fic.corpuslab.modules.researchgroup.services;

import java.util.List;

import es.udc.fic.corpuslab.modules.researchgroup.dtos.ResearchGroupSummaryDto;

public interface ResearchGroupService {

    List<ResearchGroupSummaryDto> findMyResearchGroups(String authenticatedEmail);
}
