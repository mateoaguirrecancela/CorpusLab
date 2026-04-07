package es.udc.fic.corpuslab.modules.project.exceptions;

public class ProjectNotFoundException extends RuntimeException {

    private final Long id;

    public ProjectNotFoundException(Long id) {
        super("Project not found with id: " + id);
        this.id = id;
    }

    public Long getId() {
        return id;
    }
}
