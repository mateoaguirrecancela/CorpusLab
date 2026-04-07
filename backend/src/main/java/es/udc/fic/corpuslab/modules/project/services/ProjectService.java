package es.udc.fic.corpuslab.modules.project.services;

import es.udc.fic.corpuslab.modules.project.dtos.CreateProjectRequestDto;
import es.udc.fic.corpuslab.modules.project.dtos.ProjectSummaryDto;

public interface ProjectService {

    ProjectSummaryDto createProject(String authenticatedEmail, Long researchGroupId, CreateProjectRequestDto request);
}
