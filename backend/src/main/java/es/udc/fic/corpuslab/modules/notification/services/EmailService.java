package es.udc.fic.corpuslab.modules.notification.services;

public interface EmailService {
    void sendPasswordResetEmail(String to, String resetUrl);

    void sendResearchGroupInvitationToExistingUser(
            String to,
            String groupName,
            String inviterFullName,
            String invitationUrl);

    void sendResearchGroupInvitationToNewUser(
            String to,
            String groupName,
            String inviterFullName,
            String signupUrl);
}
