package es.udc.fic.corpuslab.modules.project.shared.references;

import org.springframework.stereotype.Service;

import es.udc.fic.corpuslab.modules.auth.entities.User;
import es.udc.fic.corpuslab.modules.project.shared.entities.Annotation;
import es.udc.fic.corpuslab.modules.project.shared.entities.Project;
import es.udc.fic.corpuslab.modules.project.shared.entities.ProjectParticipant;
import es.udc.fic.corpuslab.modules.researchgroup.entities.ResearchGroup;
import jakarta.persistence.EntityManager;

@Service
public class ProjectEntityReferenceService {

    private final EntityManager entityManager;

    public ProjectEntityReferenceService(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public void attachResearchGroup(Project project, Long researchGroupId) {
        project.setResearchGroup(entityManager.getReference(ResearchGroup.class, researchGroupId));
    }

    public void attachUser(ProjectParticipant participant, Long userId) {
        participant.setUser(entityManager.getReference(User.class, userId));
    }

    public void attachUser(Annotation annotation, Long userId) {
        annotation.setUser(entityManager.getReference(User.class, userId));
    }

    public void attachWarningMarkedByUser(Annotation annotation, Long userId) {
        annotation.setWarningMarkedByUser(entityManager.getReference(User.class, userId));
    }
}
