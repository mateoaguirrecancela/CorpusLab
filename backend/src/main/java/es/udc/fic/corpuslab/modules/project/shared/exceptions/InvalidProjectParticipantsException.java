package es.udc.fic.corpuslab.modules.project.shared.exceptions;

import org.springframework.http.HttpStatus;

import es.udc.fic.corpuslab.common.exceptions.TranslatableApiException;

public class InvalidProjectParticipantsException extends RuntimeException implements TranslatableApiException {

    public InvalidProjectParticipantsException(String message) {
        super(message);
    }

    @Override
    public String getMessageKey() {
        return "project.participants.error.invalid";
    }

    @Override
    public Object[] getMessageArgs() {
        return new Object[] { getMessage() };
    }

    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.BAD_REQUEST;
    }
}
