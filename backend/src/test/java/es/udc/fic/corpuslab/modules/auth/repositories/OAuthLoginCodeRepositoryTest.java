package es.udc.fic.corpuslab.modules.auth.repositories;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import jakarta.persistence.EntityManager;

import es.udc.fic.corpuslab.modules.auth.entities.OAuthLoginCode;
import es.udc.fic.corpuslab.modules.auth.entities.User;
import es.udc.fic.corpuslab.modules.auth.fixtures.UserTestBuilder;
import es.udc.fic.corpuslab.modules.auth.utils.SecureTokenUtils;

@DataJpaTest
@ActiveProfiles("test")
class OAuthLoginCodeRepositoryTest {

    @Autowired
    private OAuthLoginCodeRepository oauthLoginCodeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void markAsConsumedIfValidShouldOnlyUpdateUnconsumedAndUnexpiredCode() {
        User user = userRepository.save(UserTestBuilder.validUser().withEmail("oauth.code@example.com").build());
        String codeHash = SecureTokenUtils.sha256("valid-code");
        OAuthLoginCode loginCode = new OAuthLoginCode();
        loginCode.setCodeHash(codeHash);
        loginCode.setProvider("google");
        loginCode.setUser(user);
        loginCode.setExpiresAt(Instant.now().plusSeconds(120));
        oauthLoginCodeRepository.save(loginCode);

        Instant consumedAt = Instant.now().truncatedTo(ChronoUnit.MICROS);
        int updatedRows = oauthLoginCodeRepository.markAsConsumedIfValid(codeHash, consumedAt);

        entityManager.flush();
        entityManager.clear();

        OAuthLoginCode consumedCode = oauthLoginCodeRepository.findByCodeHash(codeHash).orElseThrow();
        int secondUpdateRows = oauthLoginCodeRepository.markAsConsumedIfValid(codeHash, Instant.now().plusSeconds(1));

        assertThat(updatedRows).isEqualTo(1);
        assertThat(consumedCode.getConsumedAt()).isEqualTo(consumedAt);
        assertThat(secondUpdateRows).isZero();
    }
}
