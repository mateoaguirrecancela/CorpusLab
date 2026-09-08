package es.udc.fic.corpuslab.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.AuthenticationException;

class OAuth2LoginFailureHandlerTest {

    @Test
    void onAuthenticationFailureShouldRedirectWithOauthErrorCode() throws Exception {
        OAuth2LoginFailureHandler handler = new OAuth2LoginFailureHandler("http://localhost:5173/auth/login");

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        AuthenticationException exception = new AuthenticationException("oauth failed") {
        };

        handler.onAuthenticationFailure(request, response, exception);

        assertThat(response.getRedirectedUrl())
                .isEqualTo("http://localhost:5173/auth/login?oauthError=authentication_failed");
    }
}
