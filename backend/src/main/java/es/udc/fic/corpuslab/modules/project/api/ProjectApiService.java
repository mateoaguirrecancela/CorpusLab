package es.udc.fic.corpuslab.modules.project.api;

/**
 * Public API contract for the project module.
 * Other modules (e.g. researchgroup) MUST use this interface
 * instead of importing Project entities or project repositories directly.
 */
public interface ProjectApiService {

    /**
     * Returns the number of projects belonging to a research group.
     */
    long countProjectsByResearchGroupId(Long researchGroupId);

    /**
     * Deletes all projects (and their cascaded data: participants, dataset items,
     * annotations) belonging to a research group.
     */
    void deleteAllProjectsByResearchGroupId(Long researchGroupId);
}
