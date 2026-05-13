package es.udc.fic.corpuslab.modules.project.services;

import org.springframework.data.domain.Slice;

import es.udc.fic.corpuslab.modules.project.dtos.CreateProjectRequestDto;
import es.udc.fic.corpuslab.modules.project.dtos.ProjectAssignedSummaryDto;
import es.udc.fic.corpuslab.modules.project.dtos.ProjectDetailDto;
import es.udc.fic.corpuslab.modules.project.dtos.ProjectSetupRequestDto;
import es.udc.fic.corpuslab.modules.project.dtos.ProjectSetupResponseDto;
import es.udc.fic.corpuslab.modules.project.dtos.ProjectSummaryDto;
import es.udc.fic.corpuslab.modules.project.dtos.UpdateProjectRequestDto;

public interface ProjectService {

    Slice<ProjectAssignedSummaryDto> findAssignedProjectsByResearchGroup(String authenticatedEmail,
            Long researchGroupId, int page, int size, boolean showArchived);

    Slice<ProjectAssignedSummaryDto> findMyAssignedProjects(String authenticatedEmail, int page, int size,
            boolean showArchived);

    ProjectDetailDto getAssignedProjectDetail(String authenticatedEmail, Long projectId);

    ProjectSummaryDto createProject(String authenticatedEmail, Long researchGroupId, CreateProjectRequestDto request);

    ProjectDetailDto updateProject(String authenticatedEmail, Long researchGroupId, Long projectId,
            UpdateProjectRequestDto request);

    void deleteProject(String authenticatedEmail, Long researchGroupId, Long projectId);

    void cleanupIncompleteProject(String authenticatedEmail, Long researchGroupId, Long projectId);

    ProjectDetailDto archiveProject(String authenticatedEmail, Long projectId);

    ProjectDetailDto unarchiveProject(String authenticatedEmail, Long projectId);

    ProjectSetupResponseDto configureProjectSetup(String authenticatedEmail, Long researchGroupId, Long projectId,
            ProjectSetupRequestDto request);

}
