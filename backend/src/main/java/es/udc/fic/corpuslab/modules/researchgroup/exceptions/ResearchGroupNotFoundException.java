package es.udc.fic.corpuslab.modules.researchgroup.exceptions;

import org.springframework.http.HttpStatus;

import es.udc.fic.corpuslab.common.exceptions.TranslatableApiException;

public class ResearchGroupNotFoundException extends RuntimeException implements TranslatableApiException {
    private final Long id;

    public ResearchGroupNotFoundException(Long id) {
        super("Research group not found with id: " + id);
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    @Override
    public String getMessageKey() {
        return "researchgroup.error.notfound";
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
