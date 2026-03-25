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
}
