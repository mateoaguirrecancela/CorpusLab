package es.udc.fic.corpuslab.modules.notification.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@Profile("!test")
public class EmailServiceImpl implements EmailService {
    private final JavaMailSender mailSender;
    private final String from;

    public EmailServiceImpl(
            JavaMailSender mailSender,
            @Value("${app.mail.from:no-reply@corpuslab.com}") String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    @Override
    public void sendPasswordResetEmail(String to, String resetUrl) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject("CorpusLab - Reset your password");
        message.setText("We received a request to reset your password.\n\n"
                + "Use this link to continue:\n"
                + resetUrl
                + "\n\nIf you did not request this change, you can ignore this email.");

        mailSender.send(message);
    }

    @Override
    public void sendResearchGroupInvitationToExistingUser(
            String to,
            String groupName,
            String inviterFullName,
            String invitationUrl) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject("CorpusLab - Research group invitation");
        message.setText("You have been invited to join the research group '" + groupName + "' by "
                + inviterFullName + ".\n\n"
                + "View your invitation here:\n"
                + invitationUrl
                + "\n\nIf you were not expecting this, you can ignore this email.");

        mailSender.send(message);
    }

    @Override
    public void sendResearchGroupInvitationToNewUser(
            String to,
            String groupName,
            String inviterFullName,
            String signupUrl) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject("CorpusLab - You were invited to a research group");
        message.setText("" + inviterFullName + " invited you to join the research group '" + groupName + "'.\n\n"
                + "Create your account to view and accept the invitation:\n"
                + signupUrl
                + "\n\nWe look forward to seeing you in CorpusLab.");

        mailSender.send(message);
    }
}
