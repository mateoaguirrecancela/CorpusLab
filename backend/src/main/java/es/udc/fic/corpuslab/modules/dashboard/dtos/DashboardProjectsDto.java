package es.udc.fic.corpuslab.modules.dashboard.dtos;

import java.util.List;

public record DashboardProjectsDto(
        List<DashboardProjectDto> recentProjects,
        List<DashboardProjectDto> advancedProjects) {

    public DashboardProjectsDto {
        recentProjects = recentProjects == null ? List.of() : List.copyOf(recentProjects);
        advancedProjects = advancedProjects == null ? List.of() : List.copyOf(advancedProjects);
    }
}
