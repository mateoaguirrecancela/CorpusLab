package es.udc.fic.corpuslab.modules.project.guideline;

import org.springframework.stereotype.Component;

import es.udc.fic.corpuslab.common.utils.StringUtils;
import es.udc.fic.corpuslab.modules.project.shared.entities.Project;

@Component
public class GuidelineFileNameGenerator {

    public String build(Project project) {
        String projectName = StringUtils.trimToNull(project.getName());
        if (projectName == null) {
            return "project-" + project.getId() + "-guideline.pdf";
        }

        String slugifiedName = projectName.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+)|(-+$)", "");

        if (slugifiedName.isBlank()) {
            return "project-" + project.getId() + "-guideline.pdf";
        }

        return slugifiedName + "-guideline.pdf";
    }
}
