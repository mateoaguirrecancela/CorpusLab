package es.udc.fic.corpuslab.modules.project.metrics;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.udc.fic.corpuslab.modules.auth.api.AuthApiService;
import es.udc.fic.corpuslab.modules.auth.api.dtos.UserInfo;
import es.udc.fic.corpuslab.modules.project.metrics.dtos.ProjectMetricsDto;
import es.udc.fic.corpuslab.modules.project.shared.exceptions.ProjectNotFoundException;
import es.udc.fic.corpuslab.modules.project.shared.repositories.ProjectParticipantRepository;

@Service
public class ProjectMetricsServiceImpl implements ProjectMetricsService {

    private final AuthApiService authApiService;
    private final ProjectParticipantRepository projectParticipantRepository;
    private final ProjectMetricsCacheService projectMetricsCacheService;

    @Autowired
    public ProjectMetricsServiceImpl(
            AuthApiService authApiService,
            ProjectParticipantRepository projectParticipantRepository,
            ProjectMetricsCacheService projectMetricsCacheService) {
        this.authApiService = authApiService;
        this.projectParticipantRepository = projectParticipantRepository;
        this.projectMetricsCacheService = projectMetricsCacheService;
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectMetricsDto getProjectMetrics(String authenticatedEmail, Long projectId) {
        UserInfo userInfo = authApiService.findUserByEmail(authenticatedEmail);
        projectParticipantRepository.findByProjectIdAndUserId(projectId, userInfo.userId())
                .orElseThrow(() -> new ProjectNotFoundException(projectId));

        return projectMetricsCacheService.getProjectMetrics(projectId);
    }
}
