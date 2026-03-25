package es.udc.fic.corpuslab.modules.auth.exceptions;

public class EmailNotFoundException extends RuntimeException {
    public EmailNotFoundException(String email) {
        super("No account found for email: " + email);
    }
}