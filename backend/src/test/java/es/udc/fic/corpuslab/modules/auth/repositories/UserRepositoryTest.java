package es.udc.fic.corpuslab.modules.auth.repositories;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import es.udc.fic.corpuslab.modules.auth.entities.User;
import es.udc.fic.corpuslab.modules.auth.fixtures.UserTestBuilder;

@DataJpaTest
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void existsByEmailIgnoreCaseShouldReturnTrueForDifferentCasing() {
        User user = UserTestBuilder.validUser().withEmail("existing.user@example.com").build();
        userRepository.save(user);

        assertThat(userRepository.existsByEmailIgnoreCase("EXISTING.USER@EXAMPLE.COM")).isTrue();
    }

    @Test
    void findByEmailIgnoreCaseShouldReturnUserForDifferentCasing() {
        User user = UserTestBuilder.validUser().withEmail("find.me@example.com").build();
        userRepository.save(user);

        assertThat(userRepository.findByEmailIgnoreCase("FIND.ME@EXAMPLE.COM"))
                .isPresent()
                .get()
                .extracting(User::getEmail)
                .isEqualTo("find.me@example.com");
    }
}
