package es.udc.fic.corpuslab.modules.project.services;

import es.udc.fic.corpuslab.modules.project.dtos.ProjectAnnotationExportCsvDto;
import es.udc.fic.corpuslab.modules.project.dtos.ProjectAnnotationWorkspaceDto;
import es.udc.fic.corpuslab.modules.project.dtos.SaveProjectAnnotationStepRequestDto;
import es.udc.fic.corpuslab.modules.project.dtos.SaveProjectAnnotationStepResponseDto;

public interface ProjectAnnotationService {

    ProjectAnnotationWorkspaceDto getAnnotationWorkspace(String authenticatedEmail, Long projectId, int offset,
            int limit);

    ProjectAnnotationWorkspaceDto getParticipantAnnotationWorkspaceForCreator(String authenticatedEmail, Long projectId,
            Long participantUserId, int offset, int limit);

    SaveProjectAnnotationStepResponseDto saveAnnotationStep(String authenticatedEmail, Long projectId,
            SaveProjectAnnotationStepRequestDto request);

    ProjectAnnotationExportCsvDto exportAnnotationResultsCsv(String authenticatedEmail, Long projectId);

}
