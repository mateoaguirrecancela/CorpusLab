package es.udc.fic.corpuslab.modules.notification.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@Profile("!test")
public class EmailServiceImpl implements EmailService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailServiceImpl.class);

    private final JavaMailSender mailSender;
    private final String from;

    public EmailServiceImpl(
            JavaMailSender mailSender,
            @Value("${app.mail.from:no-reply@corpuslab.com}") String from
    ) {
        this.mailSender = mailSender;
        this.from = from;
    }

    @Override
    public void sendPasswordResetEmail(String to, String resetUrl) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject("CorpusLab - Recuperar contraseña");
        message.setText("Hemos recibido una solicitud para restablecer tu contraseña.\n\n"
                + "Usa este enlace para continuar:\n"
                + resetUrl
                + "\n\nSi no solicitaste este cambio, puedes ignorar este correo.");

        mailSender.send(message);
        LOGGER.info("Password reset email sent from {} to {}", from, to);
    }
}
