package es.udc.fic.corpuslab.modules.project.dataset;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import es.udc.fic.corpuslab.modules.project.dataset.dtos.ProjectAnnotationSourceContentDto;
import es.udc.fic.corpuslab.modules.project.dataset.dtos.UploadProjectDatasetResponseDto;

public interface ProjectDatasetItemService {

        UploadProjectDatasetResponseDto uploadDataset(String authenticatedEmail, Long researchGroupId, Long projectId,
                        List<MultipartFile> files);

        void validateDatasetUploadRequest(String authenticatedEmail, Long researchGroupId, Long projectId,
                        List<MultipartFile> files);

        ProjectAnnotationSourceContentDto getAnnotationSourceContent(String authenticatedEmail, Long projectId,
                        Long datasetItemId);
}
