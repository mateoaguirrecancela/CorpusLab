package es.udc.fic.corpuslab.modules.project.shared.exceptions;

import org.springframework.http.HttpStatus;

import es.udc.fic.corpuslab.common.exceptions.TranslatableApiException;

public class ProjectNotFoundException extends RuntimeException implements TranslatableApiException {

    private final Long id;

    public ProjectNotFoundException(Long id) {
        super("Project not found with id: " + id);
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    @Override
    public String getMessageKey() {
        return "project.error.notfound";
    }

    @Override
    public Object[] getMessageArgs() {
        return new Object[] { id };
    }

    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.NOT_FOUND;
    }
}
