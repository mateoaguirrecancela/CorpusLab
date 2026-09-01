package es.udc.fic.corpuslab.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import jakarta.servlet.http.HttpServletRequest;

class ClientIpResolverTest {

    @Test
    void resolveShouldUseRemoteAddrWhenForwardedHeadersAreNotTrusted() {
        ClientIpResolver resolver = new ClientIpResolver(false);
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.5");
        when(request.getRemoteAddr()).thenReturn("10.0.0.1");

        assertThat(resolver.resolve(request)).isEqualTo("10.0.0.1");
    }

    @Test
    void resolveShouldUseFirstXForwardedForValueWhenTrusted() {
        ClientIpResolver resolver = new ClientIpResolver(true);
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn(" 203.0.113.5 , 70.41.3.18 ");

        assertThat(resolver.resolve(request)).isEqualTo("203.0.113.5");
    }

    @Test
    void resolveShouldFallBackToXRealIpWhenForwardedForIsMissing() {
        ClientIpResolver resolver = new ClientIpResolver(true);
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn("198.51.100.7");

        assertThat(resolver.resolve(request)).isEqualTo("198.51.100.7");
    }

    @Test
    void resolveShouldFallBackToForwardedHeaderWhenOthersAreMissing() {
        ClientIpResolver resolver = new ClientIpResolver(true);
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn("");
        when(request.getHeader("X-Real-IP")).thenReturn("   ");
        when(request.getHeader("Forwarded")).thenReturn("by=203.0.113.1; for=\"198.51.100.9\"; proto=https");

        assertThat(resolver.resolve(request)).isEqualTo("198.51.100.9");
    }

    @Test
    void resolveShouldFallBackToRemoteAddrWhenNoHeadersProvideAnIp() {
        ClientIpResolver resolver = new ClientIpResolver(true);
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getHeader("Forwarded")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("192.168.0.10");

        assertThat(resolver.resolve(request)).isEqualTo("192.168.0.10");
    }

    @Test
    void resolveShouldFallBackToRemoteAddrWhenForwardedHeaderHasNoForPart() {
        ClientIpResolver resolver = new ClientIpResolver(true);
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getHeader("Forwarded")).thenReturn("proto=https");
        when(request.getRemoteAddr()).thenReturn("192.168.0.20");

        assertThat(resolver.resolve(request)).isEqualTo("192.168.0.20");
    }

    @Test
    void resolveShouldHandleUnquotedForwardedForValue() {
        ClientIpResolver resolver = new ClientIpResolver(true);
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getHeader("Forwarded")).thenReturn("for=198.51.100.9");

        assertThat(resolver.resolve(request)).isEqualTo("198.51.100.9");
    }
}
