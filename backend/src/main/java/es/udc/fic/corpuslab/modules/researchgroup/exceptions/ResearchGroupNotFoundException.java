package es.udc.fic.corpuslab.modules.researchgroup.exceptions;

public class ResearchGroupNotFoundException extends RuntimeException {
    private final Long id;

    public ResearchGroupNotFoundException(Long id) {
        super("Research group not found with id: " + id);
        this.id = id;
    }

    public Long getId() {
        return id;
    }
}
