package es.udc.fic.corpuslab.modules.auth.services;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.udc.fic.corpuslab.modules.auth.dtos.UserLoginRequestDto;
import es.udc.fic.corpuslab.modules.auth.dtos.UserLoginResponseDto;
import es.udc.fic.corpuslab.modules.auth.dtos.UserLogoutResponseDto;
import es.udc.fic.corpuslab.modules.auth.dtos.UserProfileResponseDto;
import es.udc.fic.corpuslab.modules.auth.dtos.UserRegisterRequestDto;
import es.udc.fic.corpuslab.modules.auth.dtos.UserUpdateProfileRequestDto;
import es.udc.fic.corpuslab.modules.auth.entities.PasswordResetToken;
import es.udc.fic.corpuslab.modules.auth.entities.User;
import es.udc.fic.corpuslab.modules.auth.exceptions.EmailAlreadyRegisteredException;
import es.udc.fic.corpuslab.modules.auth.exceptions.EmailNotFoundException;
import es.udc.fic.corpuslab.modules.auth.exceptions.InvalidCredentialsException;
import es.udc.fic.corpuslab.modules.auth.exceptions.PasswordResetEmailDeliveryException;
import es.udc.fic.corpuslab.modules.auth.exceptions.PasswordResetTokenNotFoundException;
import es.udc.fic.corpuslab.modules.auth.repositories.PasswordResetTokenRepository;
import es.udc.fic.corpuslab.modules.auth.repositories.UserRepository;
import es.udc.fic.corpuslab.common.utils.EmailNormalizer;
import es.udc.fic.corpuslab.modules.auth.utils.SecureTokenUtils;
import es.udc.fic.corpuslab.modules.notification.services.EmailService;
import es.udc.fic.corpuslab.common.utils.StringUtils;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailService emailService;
    private final es.udc.fic.corpuslab.common.security.JwtTokenService jwtTokenService;
    private final OAuthLoginCodeService oAuthLoginCodeService;
    private final String frontendBaseUrl;
    private final long passwordResetTokenExpirationMinutes;

    public AuthServiceImpl(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            PasswordResetTokenRepository passwordResetTokenRepository,
            EmailService emailService,
            es.udc.fic.corpuslab.common.security.JwtTokenService jwtTokenService,
            OAuthLoginCodeService oAuthLoginCodeService,
            @Value("${app.frontend.base-url:http://localhost:5173}") String frontendBaseUrl,
            @Value("${app.auth.password-reset-token-expiration-minutes:15}") long passwordResetTokenExpirationMinutes) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.emailService = emailService;
        this.jwtTokenService = jwtTokenService;
        this.oAuthLoginCodeService = oAuthLoginCodeService;
        this.frontendBaseUrl = frontendBaseUrl;
        this.passwordResetTokenExpirationMinutes = passwordResetTokenExpirationMinutes;
    }

    @Override
    @Transactional
    public UserLoginResponseDto signup(UserRegisterRequestDto request) {
        String normalizedEmail = EmailNormalizer.canonicalizeGoogleEmail(request.email());
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new EmailAlreadyRegisteredException(normalizedEmail);
        }

        User user = new User();
        user.setEmail(normalizedEmail);
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setBirth(request.birth());
        user.setGender(request.gender());
        user.setCountryCode(normalizeCountryCode(request.countryCode()));
        user.setCity(StringUtils.trimToNull(request.city()));
        user.setPasswordHash(passwordEncoder.encode(request.password()));

        User saved = userRepository.save(user);
        String token = jwtTokenService.generateToken(saved.getEmail());
        return toLoginResponse(saved, token);
    }

    @Override
    @Transactional(readOnly = true)
    public UserLoginResponseDto login(UserLoginRequestDto request) {
        String normalizedEmail = EmailNormalizer.canonicalizeGoogleEmail(request.email());

        User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        String token = jwtTokenService.generateToken(user.getEmail());

        return toLoginResponse(user, token);
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponseDto getProfile(String authenticatedEmail) {
        String normalizedEmail = EmailNormalizer.canonicalizeGoogleEmail(authenticatedEmail);

        User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new EmailNotFoundException(normalizedEmail));

        return toProfileResponse(user);
    }

    @Override
    @Transactional
    public UserProfileResponseDto updateProfile(String authenticatedEmail, UserUpdateProfileRequestDto request) {
        String normalizedEmail = EmailNormalizer.canonicalizeGoogleEmail(authenticatedEmail);

        User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new EmailNotFoundException(normalizedEmail));

        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setBirth(request.birth());
        user.setGender(request.gender());
        user.setCountryCode(normalizeCountryCode(request.countryCode()));
        user.setCity(StringUtils.trimToNull(request.city()));

        User saved = userRepository.save(user);

        return toProfileResponse(saved);
    }

    private UserProfileResponseDto toProfileResponse(User user) {
        return new UserProfileResponseDto(
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getBirth(),
                user.getGender(),
                user.getCountryCode(),
                user.getCity());
    }

    private UserLoginResponseDto toLoginResponse(User user, String token) {
        return new UserLoginResponseDto(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getBirth(),
                user.getGender(),
                user.getCountryCode(),
                user.getCity(),
                user.getCreatedAt(),
                token);
    }

    @Override
    public UserLogoutResponseDto logout() {
        return new UserLogoutResponseDto("Logged out successfully");
    }

    @Override
    @Transactional
    public UserLoginResponseDto exchangeOAuthCode(String code) {
        User user = oAuthLoginCodeService.consumeCode(code);
        String token = jwtTokenService.generateToken(user.getEmail());

        return toLoginResponse(user, token);
    }

    @Override
    @Transactional
    public void requestPasswordReset(String email) {
        if (email == null) {
            return;
        }

        String normalizedEmail = EmailNormalizer.canonicalizeGoogleEmail(email);
        User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElse(null);

        if (user == null) {
            return;
        }

        PasswordResetToken token = passwordResetTokenRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    PasswordResetToken created = new PasswordResetToken();
                    created.setUser(user);
                    return created;
                });

        String rawToken = SecureTokenUtils.randomUrlSafeToken(32);
        token.setToken(SecureTokenUtils.sha256(rawToken));
        token.setExpiryDate(LocalDateTime.now().plus(passwordResetTokenExpirationMinutes, ChronoUnit.MINUTES));
        passwordResetTokenRepository.save(token);

        String resetUrl = frontendBaseUrl + "/auth/reset-password?token=" + rawToken;
        try {
            emailService.sendPasswordResetEmail(user.getEmail(), resetUrl);
        } catch (RuntimeException ex) {
            throw new PasswordResetEmailDeliveryException();
        }
    }

    @Override
    @Transactional
    public void resetPassword(String token, String newPassword) {
        if (token == null || token.isBlank() || newPassword == null || newPassword.isBlank()) {
            return;
        }

        PasswordResetToken passwordResetToken = passwordResetTokenRepository.findByToken(
                SecureTokenUtils.sha256(token.trim()))
                .filter(currentToken -> currentToken.getExpiryDate().isAfter(LocalDateTime.now()))
                .orElseThrow(PasswordResetTokenNotFoundException::new);

        User user = passwordResetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        passwordResetTokenRepository.delete(passwordResetToken);
    }

    private String normalizeCountryCode(String countryCode) {
        String normalized = StringUtils.trimToNull(countryCode);
        if (normalized == null) {
            return null;
        }
        return normalized.toUpperCase(Locale.ROOT);
    }
}
