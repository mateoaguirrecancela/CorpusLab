package es.udc.fic.corpuslab.modules.notification.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("test")
public class EmailServiceTest implements EmailService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailServiceTest.class);

    @Override
    public void sendPasswordResetEmail(String to, String resetUrl) {
        LOGGER.info("[TEST] Simulated email to {} with reset URL: {}", to, resetUrl);
    }

    @Override
    public void sendResearchGroupInvitationToExistingUser(
            String to,
            String groupName,
            String inviterFullName,
            String invitationUrl) {
        LOGGER.info("[TEST] Simulated existing-user invitation email to {} for group {} by {}. URL: {}",
                to, groupName, inviterFullName, invitationUrl);
    }

    @Override
    public void sendResearchGroupInvitationToNewUser(
            String to,
            String groupName,
            String inviterFullName,
            String signupUrl) {
        LOGGER.info("[TEST] Simulated new-user invitation email to {} for group {} by {}. Signup URL: {}",
                to, groupName, inviterFullName, signupUrl);
    }
}
