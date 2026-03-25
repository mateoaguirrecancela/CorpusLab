package es.udc.fic.corpuslab.modules.notification.services;

public interface EmailService {
    void sendPasswordResetEmail(String to, String resetUrl);
}
