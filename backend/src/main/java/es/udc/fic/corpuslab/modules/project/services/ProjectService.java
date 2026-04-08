package es.udc.fic.corpuslab.modules.project.services;

import es.udc.fic.corpuslab.modules.project.dtos.CreateProjectRequestDto;
import es.udc.fic.corpuslab.modules.project.dtos.ProjectSetupRequestDto;
import es.udc.fic.corpuslab.modules.project.dtos.ProjectSetupResponseDto;
import es.udc.fic.corpuslab.modules.project.dtos.ProjectSummaryDto;
import es.udc.fic.corpuslab.modules.project.dtos.UploadProjectDatasetResponseDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ProjectService {

    ProjectSummaryDto createProject(String authenticatedEmail, Long researchGroupId, CreateProjectRequestDto request);

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
}
