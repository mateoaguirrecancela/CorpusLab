package es.udc.fic.corpuslab.modules.researchgroup.exceptions;

import org.springframework.http.HttpStatus;

import es.udc.fic.corpuslab.common.exceptions.TranslatableApiException;

public class ResearchGroupInvitationCodeNotFoundException extends RuntimeException implements TranslatableApiException {
    private final String code;

    public ResearchGroupInvitationCodeNotFoundException(String code) {
        super("Invitation code not found: " + code);
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    @Override
    public String getMessageKey() {
        return "researchgroup.invitation.code.notfound";
    }

    @Override
    public Object[] getMessageArgs() {
        return new Object[] { code };
    }

    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.NOT_FOUND;
    }
}
