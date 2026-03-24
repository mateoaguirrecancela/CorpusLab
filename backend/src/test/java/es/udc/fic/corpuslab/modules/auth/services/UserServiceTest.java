package es.udc.fic.corpuslab.modules.auth.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import es.udc.fic.corpuslab.modules.auth.dtos.UserRegisterRequestDto;
import es.udc.fic.corpuslab.modules.auth.dtos.UserRegisterResponseDto;
import es.udc.fic.corpuslab.modules.auth.entities.User;
import es.udc.fic.corpuslab.modules.auth.exceptions.EmailAlreadyRegisteredException;
import es.udc.fic.corpuslab.modules.auth.repositories.UserRepository;
import es.udc.fic.corpuslab.modules.auth.testing.fixtures.UserRegisterRequestTestBuilder;
import es.udc.fic.corpuslab.modules.auth.testing.fixtures.UserTestBuilder;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, passwordEncoder);
    }

    @Test
    void signupShouldPersistNormalizedUserAndReturnResponse() {
        UserRegisterRequestDto request = UserRegisterRequestTestBuilder.validRequest()
                .withEmail("  NEW.USER@EXAMPLE.COM  ")
                .withFirstName("  New  ")
                .withLastName("  User  ")
                .withCountryCode("es")
                .withCity("  A Coruna  ")
                .build();

        User saved = UserTestBuilder.validUser()
                .withEmail("new.user@example.com")
                .withFirstName("New")
                .withLastName("User")
                .withCountryCode("ES")
                .withCity("A Coruna")
                .build();
        Instant createdAt = Instant.parse("2026-03-25T10:15:30Z");

        when(userRepository.existsByEmailIgnoreCase("new.user@example.com")).thenReturn(false);
        when(passwordEncoder.encode(request.password())).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            return savedWithIdAndCreatedAt(user, 42L, createdAt);
        });

        UserRegisterResponseDto response = userService.signup(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User persisted = userCaptor.getValue();

        assertThat(persisted.getEmail()).isEqualTo("new.user@example.com");
        assertThat(persisted.getFirstName()).isEqualTo("New");
        assertThat(persisted.getLastName()).isEqualTo("User");
        assertThat(persisted.getCountryCode()).isEqualTo("ES");
        assertThat(persisted.getCity()).isEqualTo("A Coruna");
        assertThat(persisted.getPasswordHash()).isEqualTo("encoded-password");

        assertThat(response.id()).isEqualTo(42L);
        assertThat(response.email()).isEqualTo("new.user@example.com");
        assertThat(response.firstName()).isEqualTo("New");
        assertThat(response.lastName()).isEqualTo("User");
        assertThat(response.createdAt()).isEqualTo(createdAt);
    }

    @Test
    void signupShouldFailWhenEmailAlreadyExistsIgnoringCase() {
        UserRegisterRequestDto request = UserRegisterRequestTestBuilder.validRequest()
                .withEmail(" Existing@Example.com ")
                .build();

        when(userRepository.existsByEmailIgnoreCase("existing@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.signup(request))
                .isInstanceOf(EmailAlreadyRegisteredException.class)
                .hasMessage("Email already registered: existing@example.com");

        verify(userRepository, never()).save(any(User.class));
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void signupShouldNormalizeOptionalFieldsToNullWhenBlank() {
        UserRegisterRequestDto request = UserRegisterRequestTestBuilder.validRequest()
                .withCountryCode("   ")
                .withCity("   ")
                .build();

        when(userRepository.existsByEmailIgnoreCase("new.user@example.com")).thenReturn(false);
        when(passwordEncoder.encode(request.password())).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> savedWithIdAndCreatedAt(
                invocation.getArgument(0),
                7L,
                Instant.parse("2026-03-25T00:00:00Z")
        ));

        userService.signup(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getCountryCode()).isNull();
        assertThat(userCaptor.getValue().getCity()).isNull();
    }

    private static User savedWithIdAndCreatedAt(User baseUser, Long id, Instant createdAt) {
        User saved = UserTestBuilder.validUser()
                .withEmail(baseUser.getEmail())
                .withFirstName(baseUser.getFirstName())
                .withLastName(baseUser.getLastName())
                .withBirth(baseUser.getBirth())
                .withGender(baseUser.getGender())
                .withCountryCode(baseUser.getCountryCode())
                .withCity(baseUser.getCity())
                .withPasswordHash(baseUser.getPasswordHash())
                .build();
        try {
            var idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(saved, id);

            var createdAtField = User.class.getDeclaredField("createdAt");
            createdAtField.setAccessible(true);
            createdAtField.set(saved, createdAt);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Unable to create test user", ex);
        }
        return saved;
    }
}
