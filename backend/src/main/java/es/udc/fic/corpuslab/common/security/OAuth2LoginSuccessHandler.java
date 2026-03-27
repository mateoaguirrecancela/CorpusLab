package es.udc.fic.corpuslab.common.security;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.transaction.annotation.Transactional;

import es.udc.fic.corpuslab.modules.auth.entities.User;
import es.udc.fic.corpuslab.modules.auth.repositories.UserRepository;

@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final OAuth2AuthorizedClientService authorizedClientService;
    private final RestClient restClient;
    private final String successRedirectUrl;
    private final String failureRedirectUrl;

    public OAuth2LoginSuccessHandler(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenService jwtTokenService,
            ObjectProvider<OAuth2AuthorizedClientService> authorizedClientServiceProvider,
            @Value("${app.oauth2.success-redirect-url:http://localhost:5173/oauth2/redirect}") String successRedirectUrl,
            @Value("${app.oauth2.failure-redirect-url:http://localhost:5173/auth/login}") String failureRedirectUrl
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
        this.authorizedClientService = authorizedClientServiceProvider.getIfAvailable();
        this.restClient = RestClient.builder().build();
        this.successRedirectUrl = successRedirectUrl;
        this.failureRedirectUrl = failureRedirectUrl;
    }

    @Override
    @Transactional
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {
        if (!(authentication.getPrincipal() instanceof OAuth2User oAuth2User)) {
            response.sendRedirect(failureRedirectUrl + "?oauthError=invalid_principal");
            return;
        }

        String registrationId = resolveRegistrationId(authentication);

        String email = extractEmail(authentication, registrationId, oAuth2User.getAttributes());
        if (email == null || email.isBlank()) {
            response.sendRedirect(failureRedirectUrl + "?oauthError=missing_email");
            return;
        }

        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseGet(() -> createUserFromOAuthAttributes(normalizedEmail, oAuth2User.getAttributes()));

        String token = jwtTokenService.generateToken(user.getEmail());
        String encodedToken = URLEncoder.encode(token, StandardCharsets.UTF_8);
        response.sendRedirect(successRedirectUrl + "?token=" + encodedToken);
    }

    private User createUserFromOAuthAttributes(String email, Map<String, Object> attributes) {
        User user = new User();
        user.setEmail(email);
        user.setFirstName(resolveFirstName(attributes));
        user.setLastName(resolveLastName(attributes));
        user.setBirth(null);
        user.setGender(null);
        user.setCountryCode(null);
        user.setCity(null);
        user.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
        return userRepository.save(user);
    }

    private String resolveRegistrationId(Authentication authentication) {
        if (authentication instanceof OAuth2AuthenticationToken oauthToken) {
            return oauthToken.getAuthorizedClientRegistrationId();
        }
        return "";
    }

    private String extractEmail(Authentication authentication, String registrationId, Map<String, Object> attributes) {
        Object email = attributes.get("email");
        if (email instanceof String emailValue && !emailValue.isBlank()) {
            return emailValue;
        }

        if ("github".equalsIgnoreCase(registrationId)) {
            String githubEmail = fetchGithubPrimaryEmail(authentication, registrationId);
            if (githubEmail != null && !githubEmail.isBlank()) {
                return githubEmail;
            }
        }

        Object preferredUsername = attributes.get("preferred_username");
        if (preferredUsername instanceof String usernameValue && usernameValue.contains("@")) {
            return usernameValue;
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    private String fetchGithubPrimaryEmail(Authentication authentication, String registrationId) {
        if (authorizedClientService == null) {
            return null;
        }

        OAuth2AuthorizedClient authorizedClient = authorizedClientService.loadAuthorizedClient(
                registrationId,
                authentication.getName()
        );

        if (authorizedClient == null || authorizedClient.getAccessToken() == null) {
            return null;
        }

        try {
            ArrayList<Map<String, Object>> emails = restClient.get()
                    .uri("https://api.github.com/user/emails")
                    .header("Authorization", "Bearer " + authorizedClient.getAccessToken().getTokenValue())
                    .retrieve()
                    .body(ArrayList.class);

            if (emails == null) {
                return null;
            }

            for (Map<String, Object> item : emails) {
                Object primary = item.get("primary");
                Object verified = item.get("verified");
                Object email = item.get("email");
                if (Boolean.TRUE.equals(primary) && Boolean.TRUE.equals(verified) && email instanceof String emailValue) {
                    return emailValue;
                }
            }

            for (Map<String, Object> item : emails) {
                Object email = item.get("email");
                if (email instanceof String emailValue && !emailValue.isBlank()) {
                    return emailValue;
                }
            }
        } catch (RuntimeException ex) {
            return null;
        }

        return null;
    }

    private String resolveFirstName(Map<String, Object> attributes) {
        Object givenName = attributes.get("given_name");
        if (givenName instanceof String givenNameValue && !givenNameValue.isBlank()) {
            return givenNameValue.trim();
        }

        Object fullName = attributes.get("name");
        if (fullName instanceof String fullNameValue && !fullNameValue.isBlank()) {
            String trimmedName = fullNameValue.trim();
            int firstSpace = trimmedName.indexOf(' ');
            return firstSpace > 0 ? trimmedName.substring(0, firstSpace) : trimmedName;
        }

        return "OAuth";
    }

    private String resolveLastName(Map<String, Object> attributes) {
        Object familyName = attributes.get("family_name");
        if (familyName instanceof String familyNameValue && !familyNameValue.isBlank()) {
            return familyNameValue.trim();
        }

        Object fullName = attributes.get("name");
        if (fullName instanceof String fullNameValue && !fullNameValue.isBlank()) {
            String trimmedName = fullNameValue.trim();
            int firstSpace = trimmedName.indexOf(' ');
            if (firstSpace > 0 && firstSpace + 1 < trimmedName.length()) {
                return trimmedName.substring(firstSpace + 1).trim();
            }
        }

        return "User";
    }
}
