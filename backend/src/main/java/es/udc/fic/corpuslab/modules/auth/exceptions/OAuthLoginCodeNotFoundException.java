package es.udc.fic.corpuslab.modules.auth.exceptions;

public class OAuthLoginCodeNotFoundException extends RuntimeException {

    public OAuthLoginCodeNotFoundException() {
        super("OAuth login code not found or expired");
    }
}
