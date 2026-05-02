package es.udc.fic.corpuslab.modules.project.services;

import java.util.List;

import es.udc.fic.corpuslab.modules.project.dtos.CreateProjectRequestDto;
import es.udc.fic.corpuslab.modules.project.dtos.ProjectAssignedSummaryDto;
import es.udc.fic.corpuslab.modules.project.dtos.ProjectDetailDto;
import es.udc.fic.corpuslab.modules.project.dtos.ProjectSetupRequestDto;
import es.udc.fic.corpuslab.modules.project.dtos.ProjectSetupResponseDto;
import es.udc.fic.corpuslab.modules.project.dtos.ProjectSummaryDto;
import es.udc.fic.corpuslab.modules.project.dtos.UpdateProjectRequestDto;

public interface ProjectService {

    List<ProjectAssignedSummaryDto> findAssignedProjectsByResearchGroup(String authenticatedEmail,
            Long researchGroupId);

    List<ProjectAssignedSummaryDto> findMyAssignedProjects(String authenticatedEmail);

    ProjectDetailDto getAssignedProjectDetail(String authenticatedEmail, Long projectId);

    ProjectSummaryDto createProject(String authenticatedEmail, Long researchGroupId, CreateProjectRequestDto request);

    ProjectDetailDto updateProject(String authenticatedEmail, Long researchGroupId, Long projectId,
            UpdateProjectRequestDto request);

    void deleteProject(String authenticatedEmail, Long researchGroupId, Long projectId);

    ProjectSetupResponseDto configureProjectSetup(String authenticatedEmail, Long researchGroupId, Long projectId,
            ProjectSetupRequestDto request);

}
