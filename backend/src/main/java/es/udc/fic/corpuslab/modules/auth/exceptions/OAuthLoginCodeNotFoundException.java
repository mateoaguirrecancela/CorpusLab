package es.udc.fic.corpuslab.modules.auth.exceptions;

import org.springframework.http.HttpStatus;

import es.udc.fic.corpuslab.common.exceptions.TranslatableApiException;

public class OAuthLoginCodeNotFoundException extends RuntimeException implements TranslatableApiException {

    public OAuthLoginCodeNotFoundException() {
        super("OAuth login code not found or expired");
    }

    @Override
    public String getMessageKey() {
        return "auth.error.oauth.code.notfound";
    }

    @Override
    public Object[] getMessageArgs() {
        return null;
    }

    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.UNAUTHORIZED;
    }
}
