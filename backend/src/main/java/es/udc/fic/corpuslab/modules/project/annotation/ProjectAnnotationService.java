package es.udc.fic.corpuslab.modules.project.annotation;

import es.udc.fic.corpuslab.modules.project.annotation.dtos.ProjectAnnotationWorkspaceDto;
import es.udc.fic.corpuslab.modules.project.annotation.dtos.ProjectAnnotationWarningResponseDto;
import es.udc.fic.corpuslab.modules.project.annotation.dtos.SaveProjectAnnotationStepRequestDto;
import es.udc.fic.corpuslab.modules.project.annotation.dtos.SaveProjectAnnotationStepResponseDto;

public interface ProjectAnnotationService {

    ProjectAnnotationWorkspaceDto getAnnotationWorkspace(String authenticatedEmail, Long projectId, int offset,
            int limit);

    ProjectAnnotationWorkspaceDto getParticipantAnnotationWorkspaceForCreator(String authenticatedEmail, Long projectId,
            Long participantUserId, int offset, int limit);

    SaveProjectAnnotationStepResponseDto saveAnnotationStep(String authenticatedEmail, Long projectId,
            SaveProjectAnnotationStepRequestDto request);

    ProjectAnnotationWarningResponseDto toggleAnnotationWarning(String authenticatedEmail, Long projectId,
            Long participantUserId, Long datasetItemId, Integer stepIndex);

    ProjectAnnotationWarningResponseDto resolveOwnAnnotationWarning(String authenticatedEmail, Long projectId,
            Long datasetItemId, Integer stepIndex);

}
