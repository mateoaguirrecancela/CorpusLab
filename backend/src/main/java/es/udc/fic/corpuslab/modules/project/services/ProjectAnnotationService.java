package es.udc.fic.corpuslab.modules.project.services;

import java.io.IOException;
import java.io.OutputStream;

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

    SaveProjectAnnotationStepResponseDto toggleAnnotationWarning(String authenticatedEmail, Long projectId,
            Long participantUserId, Long datasetItemId, Integer stepIndex);

    String getAnnotationResultsCsvFileName(String authenticatedEmail, Long projectId);

    void writeAnnotationResultsCsv(String authenticatedEmail, Long projectId, OutputStream outputStream)
            throws IOException;

}
