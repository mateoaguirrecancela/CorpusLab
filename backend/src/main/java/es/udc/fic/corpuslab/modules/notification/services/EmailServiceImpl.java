package es.udc.fic.corpuslab.modules.notification.services;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("!test")
public class EmailServiceImpl implements EmailService {

    private final EmailQueue emailQueue;
    private final EmailTemplateRenderer templateRenderer;
    private final long passwordResetTokenExpirationMinutes;

    public EmailServiceImpl(
            EmailQueue emailQueue,
            EmailTemplateRenderer templateRenderer,
            @Value("${app.auth.password-reset-token-expiration-minutes:15}") long passwordResetTokenExpirationMinutes) {
        this.emailQueue = emailQueue;
        this.templateRenderer = templateRenderer;
        this.passwordResetTokenExpirationMinutes = passwordResetTokenExpirationMinutes;
    }

    @Override
    public void sendPasswordResetEmail(String to, String resetUrl) {
        enqueue(to, "CorpusLab - Reset your password", templateRenderer.render("password-reset.html", Map.of(
                "resetUrl", resetUrl,
                "expirationMinutes", passwordResetTokenExpirationMinutes)));
    }

    @Override
    public void sendResearchGroupInvitationToExistingUser(
            String to,
            String groupName,
            String inviterFullName,
            String invitationUrl) {
        enqueue(to, "CorpusLab - Research group invitation", templateRenderer.render("research-group-invitation-existing-user.html", Map.of(
                "groupName", groupName,
                "inviterFullName", inviterFullName,
                "invitationUrl", invitationUrl)));
    }

    @Override
    public void sendResearchGroupInvitationToNewUser(
            String to,
            String groupName,
            String inviterFullName,
            String signupUrl) {
        enqueue(to, "CorpusLab - You were invited to a research group", templateRenderer.render("research-group-invitation-new-user.html", Map.of(
                "groupName", groupName,
                "inviterFullName", inviterFullName,
                "signupUrl", signupUrl)));
    }

    @Override
    public void sendProjectAssignmentEmail(
            String to,
            String projectName,
            String assignerFullName,
            String projectUrl) {
        enqueue(to, "CorpusLab - New project assignment", templateRenderer.render("project-assignment.html", Map.of(
                "projectName", projectName,
                "assignerFullName", assignerFullName,
                "projectUrl", projectUrl)));
    }

    private void enqueue(String to, String subject, String content) {
        emailQueue.enqueue(new EmailJob(to, subject, content));
    }
}
