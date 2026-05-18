package es.udc.fic.corpuslab.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class ClientIpResolverTest {

    @Test
    void resolveShouldIgnoreForwardedHeadersByDefault() {
        ClientIpResolver resolver = new ClientIpResolver(false);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.5");
        request.addHeader("X-Forwarded-For", "203.0.113.10");

        String clientIp = resolver.resolve(request);

        assertThat(clientIp).isEqualTo("10.0.0.5");
    }

    @Test
    void resolveShouldUseFirstForwardedAddressWhenExplicitlyTrusted() {
        ClientIpResolver resolver = new ClientIpResolver(true);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.5");
        request.addHeader("X-Forwarded-For", "203.0.113.10, 203.0.113.11");

        String clientIp = resolver.resolve(request);

        assertThat(clientIp).isEqualTo("203.0.113.10");
    }
}
