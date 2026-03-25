package es.udc.fic.corpuslab.modules.auth.exceptions;

public class PasswordResetEmailDeliveryException extends RuntimeException {
    public PasswordResetEmailDeliveryException() {
        super("Unable to send reset email at this time");
    }
}