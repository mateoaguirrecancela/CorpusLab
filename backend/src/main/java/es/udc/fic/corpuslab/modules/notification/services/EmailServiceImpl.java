package es.udc.fic.corpuslab.modules.notification.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;

@Service
@Profile("!test")
public class EmailServiceImpl implements EmailService {
    private static final Logger logger = LoggerFactory.getLogger(EmailServiceImpl.class);
    private final JavaMailSender mailSender;
    private final String from;

    public EmailServiceImpl(
            JavaMailSender mailSender,
            @Value("${app.mail.from:no-reply@corpuslab.com}") String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    @Override
    @Async
    public void sendPasswordResetEmail(String to, String resetUrl) {
        String content = "<h2 style='color: #0f172a; margin-top: 0; font-size: 24px; font-weight: 700;'>Reset your password</h2>"
                + "<p>We received a request to reset the password for your CorpusLab account. If you didn't make this request, you can safely ignore this email.</p>"
                + "<p>Click the button below to set a new password:</p>"
                + "<div style='margin: 32px 0;'>"
                + "<a href='" + resetUrl + "' style='background-color: #4f46e5; color: #ffffff; padding: 14px 28px; text-decoration: none; border-radius: 8px; font-weight: 600; font-size: 15px; display: inline-block;'>Reset Password</a>"
                + "</div>"
                + "<p style='font-size: 14px; color: #64748b;'>This link will expire in 24 hours.</p>";

        sendHtmlEmail(to, "CorpusLab - Reset your password", content);
    }

    @Override
    @Async
    public void sendResearchGroupInvitationToExistingUser(
            String to,
            String groupName,
            String inviterFullName,
            String invitationUrl) {
        String content = "<h2 style='color: #0f172a; margin-top: 0; font-size: 24px; font-weight: 700;'>Join the research group</h2>"
                + "<p><strong>" + inviterFullName + "</strong> has invited you to collaborate in the research group <span style='color: #4f46e5; font-weight: 600;'>" + groupName + "</span>.</p>"
                + "<p>By joining, you'll be able to participate in projects, manage datasets, and collaborate with your team.</p>"
                + "<div style='margin: 32px 0;'>"
                + "<a href='" + invitationUrl + "' style='background-color: #4f46e5; color: #ffffff; padding: 14px 28px; text-decoration: none; border-radius: 8px; font-weight: 600; font-size: 15px; display: inline-block;'>Accept Invitation</a>"
                + "</div>"
                + "<p style='font-size: 14px; color: #64748b;'>If you weren't expecting this invitation, you can ignore this email.</p>";

        sendHtmlEmail(to, "CorpusLab - Research group invitation", content);
    }

    @Override
    @Async
    public void sendResearchGroupInvitationToNewUser(
            String to,
            String groupName,
            String inviterFullName,
            String signupUrl) {
        String content = "<h2 style='color: #0f172a; margin-top: 0; font-size: 24px; font-weight: 700;'>You're invited to CorpusLab</h2>"
                + "<p><strong>" + inviterFullName + "</strong> wants you to join the research group <span style='color: #4f46e5; font-weight: 600;'>" + groupName + "</span> on CorpusLab.</p>"
                + "<p>CorpusLab is a professional platform for collaborative research and data annotation.</p>"
                + "<p>Create your account to get started:</p>"
                + "<div style='margin: 32px 0;'>"
                + "<a href='" + signupUrl + "' style='background-color: #4f46e5; color: #ffffff; padding: 14px 28px; text-decoration: none; border-radius: 8px; font-weight: 600; font-size: 15px; display: inline-block;'>Create Account</a>"
                + "</div>"
                + "<p style='font-size: 14px; color: #64748b;'>We're excited to have you on board!</p>";

        sendHtmlEmail(to, "CorpusLab - You were invited to a research group", content);
    }

    @Override
    @Async
    public void sendProjectAssignmentEmail(
            String to,
            String projectName,
            String assignerFullName,
            String projectUrl) {
        String content = "<h2 style='color: #0f172a; margin-top: 0; font-size: 24px; font-weight: 700;'>New project assignment</h2>"
                + "<p><strong>" + assignerFullName + "</strong> has assigned you to the project <span style='color: #4f46e5; font-weight: 600;'>" + projectName + "</span>.</p>"
                + "<p>You can now access the workspace and start contributing to the project tasks.</p>"
                + "<div style='margin: 32px 0;'>"
                + "<a href='" + projectUrl + "' style='background-color: #4f46e5; color: #ffffff; padding: 14px 28px; text-decoration: none; border-radius: 8px; font-weight: 600; font-size: 15px; display: inline-block;'>View Project</a>"
                + "</div>"
                + "<p style='font-size: 14px; color: #64748b;'>Happy researching!</p>";

        sendHtmlEmail(to, "CorpusLab - New project assignment", content);
    }

    private void sendHtmlEmail(String to, String subject, String content) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);

            String htmlBody = "<html>"
                    + "<body style='background-color: #ffffff; font-family: \"Inter\", -apple-system, BlinkMacSystemFont, \"Segoe UI\", Roboto, sans-serif; color: #334155; line-height: 1.6; margin: 0; padding: 0;'>"
                    + "  <div style='max-width: 600px; margin: 0 auto; padding: 60px 24px;'>"
                    + "    <h1 style='color: #4f46e5; font-size: 32px; font-weight: 800; letter-spacing: -0.04em;'>CorpusLab</h1>"
                    + "    <div style='font-size: 16px; color: #475569;'>"
                    + content
                    + "    </div>"
                    + "    <div style='margin-top: 48px; padding-top: 32px; border-top: 1px solid #f1f5f9;'>"
                    + "      <p style='margin: 0; font-size: 14px; color: #64748b;'>Best regards,<br><strong style='color: #0f172a;'>The CorpusLab Team</strong></p>"
                    + "    </div>"
                    + "    <div style='margin-top: 60px; color: #94a3b8; font-size: 12px;'>"
                    + "      <p style='margin: 0;'>&copy; " + java.time.LocalDate.now().getYear() + " CorpusLab. Advanced data annotation and research management.</p>"
                    + "      <p style='margin: 8px 0 0;'>This is an automated message. Please do not reply to this email.</p>"
                    + "    </div>"
                    + "  </div>"
                    + "</body>"
                    + "</html>";

            helper.setText(htmlBody, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            logger.error("Failed to send email to {}", to, e);
            throw new RuntimeException("Error sending email", e);
        }
    }
}
