package es.udc.fic.corpuslab.common.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.transaction.annotation.Transactional;

import es.udc.fic.corpuslab.modules.auth.entities.OAuthAccount;
import es.udc.fic.corpuslab.modules.auth.entities.User;
import es.udc.fic.corpuslab.modules.auth.repositories.OAuthAccountRepository;
import es.udc.fic.corpuslab.modules.auth.repositories.UserRepository;
import es.udc.fic.corpuslab.modules.auth.services.OAuthLoginCodeService;
import es.udc.fic.corpuslab.modules.auth.utils.EmailNormalizer;

@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private static final String EMAIL_ATTRIBUTE = "email";

    private final UserRepository userRepository;
    private final OAuthAccountRepository oAuthAccountRepository;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    private final OAuthLoginCodeService oAuthLoginCodeService;
    private final OAuth2AuthorizedClientService authorizedClientService;
    private final RestClient restClient;
    private final String successRedirectUrl;
    private final String failureRedirectUrl;

    public OAuth2LoginSuccessHandler(
            UserRepository userRepository,
            OAuthAccountRepository oAuthAccountRepository,
            org.springframework.security.crypto.password.PasswordEncoder passwordEncoder,
            OAuthLoginCodeService oAuthLoginCodeService,
            ObjectProvider<OAuth2AuthorizedClientService> authorizedClientServiceProvider,
            @Value("${app.oauth2.success-redirect-url:http://localhost:5173/oauth2/redirect}") String successRedirectUrl,
            @Value("${app.oauth2.failure-redirect-url:http://localhost:5173/auth/login}") String failureRedirectUrl) {
        this.userRepository = userRepository;
        this.oAuthAccountRepository = oAuthAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.oAuthLoginCodeService = oAuthLoginCodeService;
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
            Authentication authentication) throws IOException, ServletException {
        if (!(authentication.getPrincipal() instanceof OAuth2User oAuth2User)) {
            response.sendRedirect(failureRedirectUrl + "?oauthError=invalid_principal");
            return;
        }

        String registrationId = resolveRegistrationId(authentication);
        String providerUserId = resolveProviderUserId(oAuth2User.getAttributes());
        if (providerUserId == null || providerUserId.isBlank()) {
            response.sendRedirect(failureRedirectUrl + "?oauthError=invalid_principal");
            return;
        }

        String email = extractEmail(authentication, registrationId, oAuth2User.getAttributes());
        if (email == null || email.isBlank()) {
            response.sendRedirect(failureRedirectUrl + "?oauthError=missing_email");
            return;
        }

        if (!isEmailVerified(registrationId, oAuth2User.getAttributes())) {
            response.sendRedirect(failureRedirectUrl + "?oauthError=unverified_email");
            return;
        }

        String normalizedEmail = EmailNormalizer.canonicalizeGoogleEmail(email);
        User user = oAuthAccountRepository.findByProviderAndProviderUserId(registrationId, providerUserId)
                .map(oAuthAccount -> updateOAuthAccountEmail(oAuthAccount, normalizedEmail).getUser())
                .orElseGet(() -> linkOrCreateUser(
                        registrationId,
                        providerUserId,
                        normalizedEmail,
                        oAuth2User.getAttributes()));

        String code = oAuthLoginCodeService.createCode(user, registrationId);
        response.sendRedirect(successRedirectUrl + "?code=" + encode(code) + "&provider=" + encode(registrationId));
    }

    private User linkOrCreateUser(
            String provider,
            String providerUserId,
            String email,
            Map<String, Object> attributes) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseGet(() -> createUserFromOAuthAttributes(email, attributes));

        OAuthAccount oAuthAccount = new OAuthAccount();
        oAuthAccount.setProvider(provider);
        oAuthAccount.setProviderUserId(providerUserId);
        oAuthAccount.setEmail(email);
        oAuthAccount.setUser(user);
        oAuthAccountRepository.save(oAuthAccount);

        return user;
    }

    private OAuthAccount updateOAuthAccountEmail(OAuthAccount oAuthAccount, String email) {
        if (!email.equalsIgnoreCase(oAuthAccount.getEmail())) {
            oAuthAccount.setEmail(email);
            return oAuthAccountRepository.save(oAuthAccount);
        }
        return oAuthAccount;
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
        if ("github".equalsIgnoreCase(registrationId)) {
            return fetchGithubPrimaryVerifiedEmail(authentication, registrationId);
        }

        Object email = attributes.get(EMAIL_ATTRIBUTE);
        if (email instanceof String emailValue && !emailValue.isBlank()) {
            return emailValue;
        }

        Object preferredUsername = attributes.get("preferred_username");
        if (preferredUsername instanceof String usernameValue && usernameValue.contains("@")) {
            return usernameValue;
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    private String fetchGithubPrimaryVerifiedEmail(Authentication authentication, String registrationId) {
        if (authorizedClientService == null) {
            return null;
        }

        OAuth2AuthorizedClient authorizedClient = authorizedClientService.loadAuthorizedClient(
                registrationId,
                authentication.getName());

        if (authorizedClient == null || authorizedClient.getAccessToken() == null) {
            return null;
        }

        try {
            List<Map<String, Object>> emails = restClient.get()
                    .uri("https://api.github.com/user/emails")
                    .header("Authorization", "Bearer " + authorizedClient.getAccessToken().getTokenValue())
                    .retrieve()
                    .body(List.class);

            if (emails == null) {
                return null;
            }

            for (Map<String, Object> item : emails) {
                Object primary = item.get("primary");
                Object verified = item.get("verified");
                Object email = item.get(EMAIL_ATTRIBUTE);
                if (Boolean.TRUE.equals(primary) && Boolean.TRUE.equals(verified)
                        && email instanceof String emailValue) {
                    return emailValue;
                }
            }
        } catch (RuntimeException ex) {
            return null;
        }

        return null;
    }

    private boolean isEmailVerified(String registrationId, Map<String, Object> attributes) {
        if ("github".equalsIgnoreCase(registrationId)) {
            return true;
        }

        Object emailVerified = attributes.get("email_verified");
        if (emailVerified == null) {
            return true;
        }

        return Boolean.TRUE.equals(emailVerified) || "true".equalsIgnoreCase(emailVerified.toString());
    }

    private String resolveProviderUserId(Map<String, Object> attributes) {
        Object subject = attributes.get("sub");
        if (subject instanceof String subjectValue && !subjectValue.isBlank()) {
            return subjectValue;
        }

        Object id = attributes.get("id");
        if (id != null && !id.toString().isBlank()) {
            return id.toString();
        }

        return null;
    }

    private String encode(String value) {
        return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8);
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
