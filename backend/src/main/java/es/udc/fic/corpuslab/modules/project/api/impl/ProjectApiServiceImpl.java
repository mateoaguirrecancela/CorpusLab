package es.udc.fic.corpuslab.modules.project.api.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.udc.fic.corpuslab.modules.notification.services.NotificationService;
import es.udc.fic.corpuslab.modules.project.api.ProjectApiService;
import es.udc.fic.corpuslab.modules.project.entities.Project;
import es.udc.fic.corpuslab.modules.project.repositories.AnnotationRepository;
import es.udc.fic.corpuslab.modules.project.repositories.DatasetItemRepository;
import es.udc.fic.corpuslab.modules.project.repositories.ProjectParticipantRepository;
import es.udc.fic.corpuslab.modules.project.repositories.ProjectRepository;

@Service
public class ProjectApiServiceImpl implements ProjectApiService {

    private final ProjectRepository projectRepository;
    private final ProjectParticipantRepository projectParticipantRepository;
    private final DatasetItemRepository datasetItemRepository;
    private final AnnotationRepository annotationRepository;
    private final NotificationService notificationService;

    public ProjectApiServiceImpl(
            ProjectRepository projectRepository,
            ProjectParticipantRepository projectParticipantRepository,
            DatasetItemRepository datasetItemRepository,
            AnnotationRepository annotationRepository,
            NotificationService notificationService) {
        this.projectRepository = projectRepository;
        this.projectParticipantRepository = projectParticipantRepository;
        this.datasetItemRepository = datasetItemRepository;
        this.annotationRepository = annotationRepository;
        this.notificationService = notificationService;
    }

    @Override
    @Transactional(readOnly = true)
    public long countProjectsByResearchGroupId(Long researchGroupId) {
        return projectRepository.countByResearchGroupId(researchGroupId);
    }

    @Override
    @Transactional
    public void deleteAllProjectsByResearchGroupId(Long researchGroupId) {
        List<Project> projects = projectRepository.findByResearchGroupId(researchGroupId);
        for (Project project : projects) {
            Long projectId = project.getId();
            notificationService.deleteNotificationsByProjectId(projectId);
            annotationRepository.deleteByDatasetItemProjectId(projectId);
            datasetItemRepository.deleteByProjectId(projectId);
            projectParticipantRepository.deleteByProjectId(projectId);
            projectRepository.delete(project);
        }
    }
}
