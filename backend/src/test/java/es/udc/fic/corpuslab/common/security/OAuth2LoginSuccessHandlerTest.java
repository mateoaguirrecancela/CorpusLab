package es.udc.fic.corpuslab.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import es.udc.fic.corpuslab.modules.auth.entities.User;
import es.udc.fic.corpuslab.modules.auth.entities.OAuthAccount;
import es.udc.fic.corpuslab.modules.auth.repositories.OAuthAccountRepository;
import es.udc.fic.corpuslab.modules.auth.repositories.UserRepository;
import es.udc.fic.corpuslab.modules.auth.services.OAuthLoginCodeService;

@ExtendWith(MockitoExtension.class)
class OAuth2LoginSuccessHandlerTest {

        @Mock
        private UserRepository userRepository;

        @Mock
        private OAuthAccountRepository oAuthAccountRepository;

        @Mock
        private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

        @Mock
        private OAuthLoginCodeService oAuthLoginCodeService;

        @Mock
        private ObjectProvider<OAuth2AuthorizedClientService> authorizedClientServiceProvider;

        private OAuth2LoginSuccessHandler handler;

        @BeforeEach
        void setUp() {
                when(authorizedClientServiceProvider.getIfAvailable()).thenReturn(null);
                handler = new OAuth2LoginSuccessHandler(
                                userRepository,
                                oAuthAccountRepository,
                                passwordEncoder,
                                oAuthLoginCodeService,
                                authorizedClientServiceProvider,
                                "http://localhost:5173/oauth2/redirect",
                                "http://localhost:5173/auth/login");
        }

        @Test
        void onAuthenticationSuccessShouldReuseExistingUserAndRedirectWithToken() throws Exception {
                Map<String, Object> attributes = new LinkedHashMap<>();
                attributes.put("sub", "google-subject");
                attributes.put("email", "Existing.User@Example.com");
                attributes.put("email_verified", true);
                attributes.put("given_name", "Existing");
                attributes.put("family_name", "User");

                Authentication authentication = oauthAuthentication("google", attributes);

                User existingUser = new User();
                existingUser.setEmail("existing.user@example.com");
                existingUser.setFirstName("Existing");
                existingUser.setLastName("User");
                existingUser.setPasswordHash("persisted-hash");

                OAuthAccount linkedAccount = new OAuthAccount();
                linkedAccount.setProvider("google");
                linkedAccount.setProviderUserId("google-subject");
                linkedAccount.setEmail("existing.user@example.com");
                linkedAccount.setUser(existingUser);

                when(oAuthAccountRepository.findByProviderAndProviderUserId("google", "google-subject"))
                                .thenReturn(Optional.of(linkedAccount));
                when(oAuthLoginCodeService.createCode(existingUser, "google")).thenReturn("one-time-code");

                MockHttpServletRequest request = new MockHttpServletRequest();
                MockHttpServletResponse response = new MockHttpServletResponse();

                handler.onAuthenticationSuccess(request, response, authentication);

                assertThat(response.getRedirectedUrl())
                                .isEqualTo("http://localhost:5173/oauth2/redirect?code=one-time-code&provider=google");
                assertThat(request.getSession(false)).isNull();

                verify(userRepository, never()).save(any(User.class));
                verify(passwordEncoder, never()).encode(any());
        }

        @Test
        void onAuthenticationSuccessShouldCreateUserWhenEmailDoesNotExist() throws Exception {
                Map<String, Object> attributes = new LinkedHashMap<>();
                attributes.put("sub", "google-subject");
                attributes.put("email", "new.oauth.user@example.com");
                attributes.put("email_verified", true);
                attributes.put("given_name", "New");
                attributes.put("family_name", "User");

                Authentication authentication = oauthAuthentication("google", attributes);

                when(oAuthAccountRepository.findByProviderAndProviderUserId("google", "google-subject"))
                                .thenReturn(Optional.empty());
                when(userRepository.findByEmailIgnoreCase("new.oauth.user@example.com")).thenReturn(Optional.empty());
                when(passwordEncoder.encode(any())).thenReturn("encoded-random-password");
                when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
                when(oAuthLoginCodeService.createCode(any(User.class), eq("google"))).thenReturn("fresh-login-code");
                when(oAuthAccountRepository.save(any(OAuthAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

                MockHttpServletRequest request = new MockHttpServletRequest();
                MockHttpServletResponse response = new MockHttpServletResponse();

                handler.onAuthenticationSuccess(request, response, authentication);

                ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
                verify(userRepository).save(userCaptor.capture());
                User created = userCaptor.getValue();

                assertThat(created.getEmail()).isEqualTo("new.oauth.user@example.com");
                assertThat(created.getFirstName()).isEqualTo("New");
                assertThat(created.getLastName()).isEqualTo("User");
                assertThat(created.getBirth()).isNull();
                assertThat(created.getGender()).isNull();
                assertThat(created.getCountryCode()).isNull();
                assertThat(created.getCity()).isNull();
                assertThat(created.getPasswordHash()).isEqualTo("encoded-random-password");

                assertThat(response.getRedirectedUrl())
                                .isEqualTo("http://localhost:5173/oauth2/redirect?code=fresh-login-code&provider=google");
        }

        @Test
        void onAuthenticationSuccessShouldLinkProviderToExistingUserWhenEmailAlreadyExists() throws Exception {
                Map<String, Object> attributes = new LinkedHashMap<>();
                attributes.put("sub", "second-provider-subject");
                attributes.put("email", "Same.User@gmail.com");
                attributes.put("email_verified", true);
                attributes.put("given_name", "Same");
                attributes.put("family_name", "User");

                Authentication authentication = oauthAuthentication("google", attributes);

                User existingUser = new User();
                existingUser.setEmail("sameuser@gmail.com");
                existingUser.setFirstName("Existing");
                existingUser.setLastName("User");
                existingUser.setPasswordHash("persisted-hash");

                when(oAuthAccountRepository.findByProviderAndProviderUserId("google", "second-provider-subject"))
                                .thenReturn(Optional.empty());
                when(userRepository.findByEmailIgnoreCase("sameuser@gmail.com"))
                                .thenReturn(Optional.of(existingUser));
                when(oAuthAccountRepository.save(any(OAuthAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));
                when(oAuthLoginCodeService.createCode(existingUser, "google")).thenReturn("linked-login-code");

                MockHttpServletRequest request = new MockHttpServletRequest();
                MockHttpServletResponse response = new MockHttpServletResponse();

                handler.onAuthenticationSuccess(request, response, authentication);

                ArgumentCaptor<OAuthAccount> accountCaptor = ArgumentCaptor.forClass(OAuthAccount.class);
                verify(oAuthAccountRepository).save(accountCaptor.capture());
                OAuthAccount linkedAccount = accountCaptor.getValue();

                assertThat(linkedAccount.getProvider()).isEqualTo("google");
                assertThat(linkedAccount.getProviderUserId()).isEqualTo("second-provider-subject");
                assertThat(linkedAccount.getEmail()).isEqualTo("sameuser@gmail.com");
                assertThat(linkedAccount.getUser()).isSameAs(existingUser);
                verify(userRepository, never()).save(any(User.class));
                verify(passwordEncoder, never()).encode(any());
                assertThat(response.getRedirectedUrl())
                                .isEqualTo("http://localhost:5173/oauth2/redirect?code=linked-login-code&provider=google");
        }

        @Test
        void onAuthenticationSuccessShouldRedirectToFailureWhenEmailIsMissing() throws Exception {
                Map<String, Object> attributes = Map.of("sub", "google-subject");
                Authentication authentication = oauthAuthentication("google", attributes);

                MockHttpServletRequest request = new MockHttpServletRequest();
                MockHttpServletResponse response = new MockHttpServletResponse();

                handler.onAuthenticationSuccess(request, response, authentication);

                assertThat(response.getRedirectedUrl())
                                .isEqualTo("http://localhost:5173/auth/login?oauthError=missing_email");
                verify(userRepository, never()).save(any(User.class));
                verify(oAuthLoginCodeService, never()).createCode(any(), any());
        }

        @Test
        void onAuthenticationSuccessShouldRedirectToFailureWhenPrincipalIsNotOAuthUser() throws Exception {
                Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(
                                "plain-user",
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_USER")));

                MockHttpServletRequest request = new MockHttpServletRequest();
                MockHttpServletResponse response = new MockHttpServletResponse();

                handler.onAuthenticationSuccess(request, response, authentication);

                assertThat(response.getRedirectedUrl())
                                .isEqualTo("http://localhost:5173/auth/login?oauthError=invalid_principal");
                verify(userRepository, never()).findByEmailIgnoreCase(any());
                verify(oAuthLoginCodeService, never()).createCode(any(), any());
        }

        @Test
        @SuppressWarnings("unchecked")
        void onAuthenticationSuccessShouldUseGithubPrimaryEmailWhenAttributeEmailIsMissing() throws Exception {
                OAuth2AuthorizedClientService authorizedClientService = org.mockito.Mockito
                                .mock(OAuth2AuthorizedClientService.class);
                ObjectProvider<OAuth2AuthorizedClientService> provider = (ObjectProvider<OAuth2AuthorizedClientService>) org.mockito.Mockito
                                .mock(ObjectProvider.class);
                when(provider.getIfAvailable()).thenReturn(authorizedClientService);

                OAuth2LoginSuccessHandler githubHandler = new OAuth2LoginSuccessHandler(
                                userRepository,
                                oAuthAccountRepository,
                                passwordEncoder,
                                oAuthLoginCodeService,
                                provider,
                                "http://localhost:5173/oauth2/redirect",
                                "http://localhost:5173/auth/login");

                Map<String, Object> attributes = new LinkedHashMap<>();
                attributes.put("sub", "github-subject");
                attributes.put("name", "Octo Cat");
                OAuth2User oauth2User = new DefaultOAuth2User(
                                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                                attributes,
                                "sub");
                Authentication authentication = new OAuth2AuthenticationToken(
                                oauth2User,
                                oauth2User.getAuthorities(),
                                "github");

                MockHttpServletRequest request = new MockHttpServletRequest();
                MockHttpServletResponse response = new MockHttpServletResponse();

                githubHandler.onAuthenticationSuccess(request, response, authentication);

                assertThat(response.getRedirectedUrl())
                                .isEqualTo("http://localhost:5173/auth/login?oauthError=missing_email");
                verify(userRepository, never()).save(any(User.class));
                verify(oAuthLoginCodeService, never()).createCode(any(), any());
        }

        @Test
        void onAuthenticationSuccessShouldRejectUnverifiedProviderEmail() throws Exception {
                Map<String, Object> attributes = new LinkedHashMap<>();
                attributes.put("sub", "google-subject");
                attributes.put("email", "unverified.user@example.com");
                attributes.put("email_verified", false);

                Authentication authentication = oauthAuthentication("google", attributes);

                MockHttpServletRequest request = new MockHttpServletRequest();
                MockHttpServletResponse response = new MockHttpServletResponse();

                handler.onAuthenticationSuccess(request, response, authentication);

                assertThat(response.getRedirectedUrl())
                                .isEqualTo("http://localhost:5173/auth/login?oauthError=unverified_email");
                verify(userRepository, never()).save(any(User.class));
                verify(oAuthLoginCodeService, never()).createCode(any(), any());
        }

        private Authentication oauthAuthentication(String registrationId, Map<String, Object> attributes) {
                OAuth2User oAuth2User = new DefaultOAuth2User(
                                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                                attributes,
                                "sub");
                return new OAuth2AuthenticationToken(oAuth2User, oAuth2User.getAuthorities(), registrationId);
        }
}
