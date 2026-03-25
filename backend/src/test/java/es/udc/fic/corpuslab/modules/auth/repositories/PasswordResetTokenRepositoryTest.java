package es.udc.fic.corpuslab.modules.auth.repositories;

import static org.assertj.core.api.Assertions.assertThat;

import es.udc.fic.corpuslab.modules.auth.entities.PasswordResetToken;
import es.udc.fic.corpuslab.modules.auth.entities.User;
import es.udc.fic.corpuslab.modules.auth.fixtures.UserTestBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

@DataJpaTest
@ActiveProfiles("test")
class PasswordResetTokenRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Test
    void findByTokenShouldReturnTokenWhenExists() {
        User user = UserTestBuilder.validUser().withEmail("reset.user@example.com").build();
        userRepository.save(user);

        PasswordResetToken token = new PasswordResetToken();
        token.setToken("known-token");
        token.setUser(user);
        token.setExpiryDate(LocalDateTime.now().plusMinutes(15));
        passwordResetTokenRepository.save(token);

        assertThat(passwordResetTokenRepository.findByToken("known-token"))
                .isPresent()
                .get()
                .extracting(PasswordResetToken::getToken)
                .isEqualTo("known-token");
    }
}
