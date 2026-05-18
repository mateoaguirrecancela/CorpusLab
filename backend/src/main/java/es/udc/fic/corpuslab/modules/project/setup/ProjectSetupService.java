package es.udc.fic.corpuslab.modules.project.setup;

import org.springframework.web.multipart.MultipartFile;

import es.udc.fic.corpuslab.modules.project.setup.dtos.ProjectSetupRequestDto;
import es.udc.fic.corpuslab.modules.project.setup.dtos.ProjectSetupResponseDto;

public interface ProjectSetupService {

    ProjectSetupResponseDto configureProjectSetup(
            String authenticatedEmail,
            Long researchGroupId,
            Long projectId,
            ProjectSetupRequestDto request,
            MultipartFile guidelinePdfFile);
}
