package es.udc.fic.corpuslab.modules.project.setup;

import java.util.List;

import es.udc.fic.corpuslab.modules.project.setup.dtos.ProjectSetupLabelDto;
import es.udc.fic.corpuslab.modules.project.shared.enums.ProjectType;

public interface ProjectLabelService {

    List<ProjectSetupLabelDto> getProjectLabels(Long projectId);

    List<ProjectSetupLabelDto> replaceProjectLabels(Long projectId, ProjectType projectType,
            List<ProjectSetupLabelDto> labelsInput);
}
