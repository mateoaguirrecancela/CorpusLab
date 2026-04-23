package es.udc.fic.corpuslab.modules.project.services;

import es.udc.fic.corpuslab.modules.project.dtos.CreateProjectRequestDto;
import es.udc.fic.corpuslab.modules.project.dtos.ProjectAssignedSummaryDto;
import es.udc.fic.corpuslab.modules.project.dtos.ProjectAnnotationExportCsvDto;
import es.udc.fic.corpuslab.modules.project.dtos.ProjectAnnotationSourceContentDto;
import es.udc.fic.corpuslab.modules.project.dtos.ProjectAnnotationWorkspaceDto;
import es.udc.fic.corpuslab.modules.project.dtos.ProjectDetailDto;
import es.udc.fic.corpuslab.modules.project.dtos.ProjectSetupRequestDto;
import es.udc.fic.corpuslab.modules.project.dtos.ProjectSetupResponseDto;
import es.udc.fic.corpuslab.modules.project.dtos.ProjectSummaryDto;
import es.udc.fic.corpuslab.modules.project.dtos.SaveProjectAnnotationStepRequestDto;
import es.udc.fic.corpuslab.modules.project.dtos.SaveProjectAnnotationStepResponseDto;
import es.udc.fic.corpuslab.modules.project.dtos.UpdateProjectRequestDto;
import es.udc.fic.corpuslab.modules.project.dtos.UploadProjectDatasetResponseDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ProjectService {

        List<ProjectAssignedSummaryDto> findAssignedProjectsByResearchGroup(
                        String authenticatedEmail,
                        Long researchGroupId);

        List<ProjectAssignedSummaryDto> findMyAssignedProjects(String authenticatedEmail);

        ProjectDetailDto getAssignedProjectDetail(String authenticatedEmail, Long projectId);

        ProjectSummaryDto createProject(String authenticatedEmail, Long researchGroupId,
                        CreateProjectRequestDto request);

        ProjectDetailDto updateProject(
                        String authenticatedEmail,
                        Long researchGroupId,
                        Long projectId,
                        UpdateProjectRequestDto request);

        void deleteProject(
                        String authenticatedEmail,
                        Long researchGroupId,
                        Long projectId);

        UploadProjectDatasetResponseDto uploadDataset(
                        String authenticatedEmail,
                        Long researchGroupId,
                        Long projectId,
                        List<MultipartFile> files);

        ProjectSetupResponseDto configureProjectSetup(
                        String authenticatedEmail,
                        Long researchGroupId,
                        Long projectId,
                        ProjectSetupRequestDto request);

        void assignParticipants(
                        String authenticatedEmail,
                        Long researchGroupId,
                        Long projectId,
                        List<Long> participantUserIds);

        ProjectAnnotationWorkspaceDto getAnnotationWorkspace(
                        String authenticatedEmail,
                        Long projectId,
                        int offset,
                        int limit);

        ProjectAnnotationWorkspaceDto getParticipantAnnotationWorkspaceForCreator(
                        String authenticatedEmail,
                        Long projectId,
                        Long participantUserId,
                        int offset,
                        int limit);

        ProjectAnnotationSourceContentDto getAnnotationSourceContent(
                        String authenticatedEmail,
                        Long projectId,
                        Long datasetItemId);

        ProjectAnnotationExportCsvDto exportAnnotationResultsCsv(
                        String authenticatedEmail,
                        Long projectId);

        SaveProjectAnnotationStepResponseDto saveAnnotationStep(
                        String authenticatedEmail,
                        Long projectId,
                        SaveProjectAnnotationStepRequestDto request);

        void removeParticipantFromAllGroupProjects(
                        Long researchGroupId,
                        Long userId);
}
