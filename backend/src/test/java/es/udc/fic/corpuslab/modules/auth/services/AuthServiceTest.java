package es.udc.fic.corpuslab.modules.auth.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import es.udc.fic.corpuslab.modules.auth.dtos.UserLoginRequestDto;
import es.udc.fic.corpuslab.modules.auth.dtos.UserLoginResponseDto;
import es.udc.fic.corpuslab.modules.auth.dtos.UserProfileResponseDto;
import es.udc.fic.corpuslab.modules.auth.dtos.UserUpdateProfileRequestDto;
import es.udc.fic.corpuslab.modules.auth.dtos.UserRegisterRequestDto;
import es.udc.fic.corpuslab.modules.auth.dtos.UserRegisterResponseDto;
import es.udc.fic.corpuslab.modules.auth.dtos.UserLogoutResponseDto;
import es.udc.fic.corpuslab.modules.auth.entities.PasswordResetToken;
import es.udc.fic.corpuslab.modules.auth.entities.User;
import es.udc.fic.corpuslab.modules.auth.exceptions.EmailAlreadyRegisteredException;
import es.udc.fic.corpuslab.modules.auth.exceptions.EmailNotFoundException;
import es.udc.fic.corpuslab.modules.auth.exceptions.InvalidCredentialsException;
import es.udc.fic.corpuslab.modules.auth.exceptions.PasswordResetEmailDeliveryException;
import es.udc.fic.corpuslab.modules.auth.exceptions.PasswordResetTokenNotFoundException;
import es.udc.fic.corpuslab.modules.auth.fixtures.UserLoginRequestTestBuilder;
import es.udc.fic.corpuslab.modules.auth.fixtures.UserRegisterRequestTestBuilder;
import es.udc.fic.corpuslab.modules.auth.fixtures.UserTestBuilder;
import es.udc.fic.corpuslab.modules.auth.fixtures.UserUpdateProfileRequestTestBuilder;
import es.udc.fic.corpuslab.modules.auth.repositories.PasswordResetTokenRepository;
import es.udc.fic.corpuslab.modules.auth.repositories.UserRepository;
import es.udc.fic.corpuslab.modules.auth.utils.SecureTokenUtils;
import es.udc.fic.corpuslab.modules.notification.services.EmailService;
import es.udc.fic.corpuslab.common.security.JwtTokenService;

import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private JwtTokenService jwtTokenService;

    @Mock
    private OAuthLoginCodeService oAuthLoginCodeService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(
                userRepository,
                passwordEncoder,
                passwordResetTokenRepository,
                emailService,
                jwtTokenService,
                oAuthLoginCodeService,
                "http://frontend.test");
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

        Instant createdAt = Instant.parse("2026-03-25T10:15:30Z");

        when(userRepository.existsByEmailIgnoreCase("new.user@example.com")).thenReturn(false);
        when(passwordEncoder.encode(request.password())).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            return savedWithIdAndCreatedAt(user, 42L, createdAt);
        });

        UserRegisterResponseDto response = authService.signup(request);

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

        assertThatThrownBy(() -> authService.signup(request))
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
                Instant.parse("2026-03-25T00:00:00Z")));

        authService.signup(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getCountryCode()).isNull();
        assertThat(userCaptor.getValue().getCity()).isNull();
    }

    @Test
    void loginShouldAuthenticateAndReturnToken() {

        User user = UserTestBuilder.validUser().withEmail("new.user@example.com").build();
        setId(user, 99L);

        UserLoginRequestDto request = UserLoginRequestTestBuilder.validRequest()
                .withEmail(" NEW.USER@EXAMPLE.COM ")
                .withPassword("strong-password")
                .build();

        when(userRepository.findByEmailIgnoreCase("new.user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("strong-password", user.getPasswordHash())).thenReturn(true);
        when(jwtTokenService.generateToken("new.user@example.com")).thenReturn("jwt-token");

        UserLoginResponseDto response = authService.login(request);

        assertThat(response.id()).isEqualTo(99L);
        assertThat(response.email()).isEqualTo("new.user@example.com");
        assertThat(response.firstName()).isEqualTo("New");
        assertThat(response.lastName()).isEqualTo("User");
        assertThat(response.token()).isEqualTo("jwt-token");
    }

    @Test
    void loginShouldFailWhenUserDoesNotExist() {
        UserLoginRequestDto request = UserLoginRequestTestBuilder.validRequest().build();

        when(userRepository.findByEmailIgnoreCase("new.user@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid email or password");
    }

    @Test
    void loginShouldFailWhenPasswordDoesNotMatch() {
        User user = UserTestBuilder.validUser().build();
        UserLoginRequestDto request = UserLoginRequestTestBuilder.validRequest().build();

        when(userRepository.findByEmailIgnoreCase("new.user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("strong-password", user.getPasswordHash())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid email or password");
    }

    @Test
    void getProfileShouldReturnMappedProfileForExistingUser() {
        User user = UserTestBuilder.validUser()
                .withEmail("new.user@example.com")
                .withFirstName("New")
                .withLastName("User")
                .withCountryCode("ES")
                .withCity("A Coruna")
                .build();

        when(userRepository.findByEmailIgnoreCase("new.user@example.com")).thenReturn(Optional.of(user));

        UserProfileResponseDto response = authService.getProfile(" NEW.USER@EXAMPLE.COM ");

        assertThat(response.email()).isEqualTo("new.user@example.com");
        assertThat(response.firstName()).isEqualTo("New");
        assertThat(response.lastName()).isEqualTo("User");
        assertThat(response.birth()).isEqualTo(user.getBirth());
        assertThat(response.gender()).isEqualTo(user.getGender());
        assertThat(response.countryCode()).isEqualTo("ES");
        assertThat(response.city()).isEqualTo("A Coruna");
    }

    @Test
    void getProfileShouldThrowWhenUserDoesNotExist() {
        when(userRepository.findByEmailIgnoreCase("missing.user@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.getProfile(" MISSING.USER@EXAMPLE.COM "))
                .isInstanceOf(EmailNotFoundException.class)
                .hasMessage("No account found for email: missing.user@example.com");
    }

    @Test
    void updateProfileShouldPersistAllowedFieldsAndReturnResponse() {
        User existingUser = UserTestBuilder.validUser()
                .withEmail("new.user@example.com")
                .withFirstName("New")
                .withLastName("User")
                .withBirth(java.time.LocalDate.of(1997, 5, 20))
                .withGender(es.udc.fic.corpuslab.modules.auth.enums.GenderType.OTHER)
                .withCountryCode("ES")
                .withCity("A Coruna")
                .build();

        UserUpdateProfileRequestDto request = UserUpdateProfileRequestTestBuilder.validRequest()
                .withFirstName("  Alice  ")
                .withLastName("  Smith  ")
                .withCountryCode("pt")
                .withCity("  ")
                .build();

        when(userRepository.findByEmailIgnoreCase("new.user@example.com")).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserProfileResponseDto response = authService.updateProfile(" NEW.USER@EXAMPLE.COM ", request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getFirstName()).isEqualTo("Alice");
        assertThat(savedUser.getLastName()).isEqualTo("Smith");
        assertThat(savedUser.getBirth()).isEqualTo(request.birth());
        assertThat(savedUser.getGender()).isEqualTo(request.gender());
        assertThat(savedUser.getCountryCode()).isEqualTo("PT");
        assertThat(savedUser.getCity()).isNull();

        assertThat(response.email()).isEqualTo("new.user@example.com");
        assertThat(response.firstName()).isEqualTo("Alice");
        assertThat(response.lastName()).isEqualTo("Smith");
        assertThat(response.birth()).isEqualTo(request.birth());
        assertThat(response.gender()).isEqualTo(request.gender());
        assertThat(response.countryCode()).isEqualTo("PT");
        assertThat(response.city()).isNull();
    }

    @Test
    void updateProfileShouldThrowWhenUserDoesNotExist() {
        UserUpdateProfileRequestDto request = UserUpdateProfileRequestTestBuilder.validRequest().build();

        when(userRepository.findByEmailIgnoreCase("missing.user@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.updateProfile(" MISSING.USER@EXAMPLE.COM ", request))
                .isInstanceOf(EmailNotFoundException.class)
                .hasMessage("No account found for email: missing.user@example.com");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void logoutShouldReturnSuccessMessage() {
        UserLogoutResponseDto response = authService.logout();
        assertThat(response.message()).isEqualTo("Logged out successfully");
    }

    @Test
    void logoutShouldBeIdempotent() {
        UserLogoutResponseDto firstResponse = authService.logout();
        UserLogoutResponseDto secondResponse = authService.logout();

        assertThat(firstResponse.message()).isEqualTo("Logged out successfully");
        assertThat(secondResponse.message()).isEqualTo("Logged out successfully");
    }

    @Test
    void requestPasswordResetShouldStoreTokenAndSendEmailForExistingUser() {
        User user = UserTestBuilder.validUser().withEmail("new.user@example.com").build();
        setId(user, 15L);

        when(userRepository.findByEmailIgnoreCase("new.user@example.com")).thenReturn(Optional.of(user));
        when(passwordResetTokenRepository.findByUserId(15L)).thenReturn(Optional.empty());

        authService.requestPasswordReset(" NEW.USER@EXAMPLE.COM ");

        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(passwordResetTokenRepository).save(tokenCaptor.capture());
        PasswordResetToken savedToken = tokenCaptor.getValue();

        assertThat(savedToken.getUser()).isEqualTo(user);
        assertThat(savedToken.getToken()).isNotBlank();
        assertThat(savedToken.getToken()).hasSize(64);
        assertThat(savedToken.getExpiryDate()).isAfter(LocalDateTime.now());
        verify(emailService).sendPasswordResetEmail(eq("new.user@example.com"), any(String.class));
    }

    @Test
    void requestPasswordResetShouldReuseExistingTokenForUser() {
        User user = UserTestBuilder.validUser().withEmail("new.user@example.com").build();
        setId(user, 15L);

        PasswordResetToken existingToken = new PasswordResetToken();
        existingToken.setToken("old-token");
        existingToken.setUser(user);
        existingToken.setExpiryDate(LocalDateTime.now().plusMinutes(1));

        when(userRepository.findByEmailIgnoreCase("new.user@example.com")).thenReturn(Optional.of(user));
        when(passwordResetTokenRepository.findByUserId(15L)).thenReturn(Optional.of(existingToken));

        authService.requestPasswordReset("new.user@example.com");

        verify(passwordResetTokenRepository).save(existingToken);
        assertThat(existingToken.getToken()).isNotEqualTo("old-token");
        assertThat(existingToken.getExpiryDate()).isAfter(LocalDateTime.now());
    }

    @Test
    void requestPasswordResetShouldThrowWhenUserDoesNotExist() {
        when(userRepository.findByEmailIgnoreCase("missing.user@example.com")).thenReturn(Optional.empty());

        authService.requestPasswordReset("missing.user@example.com");

        verify(passwordResetTokenRepository, never()).save(any(PasswordResetToken.class));
        verify(emailService, never()).sendPasswordResetEmail(any(), any());
    }

    @Test
    void requestPasswordResetShouldThrowWhenEmailDeliveryFails() {
        User user = UserTestBuilder.validUser().withEmail("new.user@example.com").build();
        setId(user, 15L);

        when(userRepository.findByEmailIgnoreCase("new.user@example.com")).thenReturn(Optional.of(user));
        when(passwordResetTokenRepository.findByUserId(15L)).thenReturn(Optional.empty());
        doThrow(new RuntimeException("smtp down"))
                .when(emailService)
                .sendPasswordResetEmail(eq("new.user@example.com"), any(String.class));

        assertThatThrownBy(() -> authService.requestPasswordReset("new.user@example.com"))
                .isInstanceOf(PasswordResetEmailDeliveryException.class)
                .hasMessage("Unable to send reset email at this time");

        verify(passwordResetTokenRepository).save(any(PasswordResetToken.class));
    }

    @Test
    void resetPasswordShouldUpdatePasswordAndDeleteTokenWhenTokenIsValid() {
        User user = UserTestBuilder.validUser().build();
        PasswordResetToken token = new PasswordResetToken();
        token.setToken("valid-token-example");
        token.setUser(user);
        token.setExpiryDate(LocalDateTime.now().plusMinutes(10));

        when(passwordResetTokenRepository.findByToken(SecureTokenUtils.sha256("valid-token-example")))
                .thenReturn(Optional.of(token));
        when(passwordEncoder.encode("my-new-password")).thenReturn("encoded-new-password");

        authService.resetPassword("valid-token-example", "my-new-password");

        assertThat(user.getPasswordHash()).isEqualTo("encoded-new-password");
        verify(userRepository).save(user);
        verify(passwordResetTokenRepository).delete(token);
    }

    @Test
    void resetPasswordShouldThrowWhenTokenIsInvalid() {
        when(passwordResetTokenRepository.findByToken(SecureTokenUtils.sha256("invalid-token"))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.resetPassword("invalid-token", "new-password"))
                .isInstanceOf(PasswordResetTokenNotFoundException.class)
                .hasMessage("Password reset token not found or expired");

        verify(userRepository, never()).save(any(User.class));
        verify(passwordResetTokenRepository, never()).delete(any(PasswordResetToken.class));
    }

    @Test
    void resetPasswordShouldThrowWhenTokenIsExpired() {
        User user = UserTestBuilder.validUser().build();
        PasswordResetToken token = new PasswordResetToken();
        token.setToken("expired-token-example");
        token.setUser(user);
        token.setExpiryDate(LocalDateTime.now().minusMinutes(1));

        when(passwordResetTokenRepository.findByToken(SecureTokenUtils.sha256("expired-token-example")))
                .thenReturn(Optional.of(token));

        assertThatThrownBy(() -> authService.resetPassword("expired-token-example", "new-password"))
                .isInstanceOf(PasswordResetTokenNotFoundException.class)
                .hasMessage("Password reset token not found or expired");

        verify(userRepository, never()).save(any(User.class));
        verify(passwordResetTokenRepository, never()).delete(any(PasswordResetToken.class));
    }

    private static void setId(User user, Long id) {
        try {
            Field idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(user, id);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Unable to set id in test", ex);
        }
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
