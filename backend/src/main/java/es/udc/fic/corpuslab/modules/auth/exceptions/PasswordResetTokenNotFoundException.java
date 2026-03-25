package es.udc.fic.corpuslab.modules.auth.exceptions;

public class PasswordResetTokenNotFoundException extends RuntimeException {
    public PasswordResetTokenNotFoundException() {
        super("Password reset token not found or expired");
    }
}