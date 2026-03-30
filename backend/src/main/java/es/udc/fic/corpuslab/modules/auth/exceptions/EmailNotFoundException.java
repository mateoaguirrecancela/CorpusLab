package es.udc.fic.corpuslab.modules.auth.exceptions;

public class EmailNotFoundException extends RuntimeException {
    private final String email;

    public EmailNotFoundException(String email) {
        super("No account found for email: " + email);
        this.email = email;
    }

    public String getEmail() {
        return email;
    }
}