package es.udc.fic.corpuslab.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

@ExtendWith(MockitoExtension.class)
class JwtTokenServiceTest {

    @Mock
    private JwtEncoder jwtEncoder;

    private JwtTokenService tokenService;

    @BeforeEach
    void setUp() {
        tokenService = new JwtTokenService(jwtEncoder, 600L);
    }

    @Test
    void generateTokenShouldEncodeExpectedClaimsAndReturnTokenValue() {
        Jwt encodedJwt = Jwt.withTokenValue("signed-token")
                .headers(headers -> headers.put("alg", "HS256"))
                .claims(claims -> claims.put("sub", "annotator@example.com"))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(600))
                .build();
        when(jwtEncoder.encode(org.mockito.ArgumentMatchers.any(JwtEncoderParameters.class)))
                .thenReturn(encodedJwt);

        String token = tokenService.generateToken("annotator@example.com");

        assertThat(token).isEqualTo("signed-token");

        ArgumentCaptor<JwtEncoderParameters> captor = ArgumentCaptor.forClass(JwtEncoderParameters.class);
        verify(jwtEncoder).encode(captor.capture());

        JwtEncoderParameters parameters = captor.getValue();
        assertThat(parameters.getJwsHeader().getAlgorithm().getName()).isEqualTo("HS256");
        assertThat(parameters.getClaims().getClaimAsString("iss")).isEqualTo("corpuslab");
        assertThat(parameters.getClaims().getSubject()).isEqualTo("annotator@example.com");
        assertThat(parameters.getClaims().getClaimAsString("email")).isEqualTo("annotator@example.com");
        assertThat(parameters.getClaims().getClaimAsString("scope")).isEqualTo("ROLE_USER");
        assertThat(parameters.getClaims().getExpiresAt()).isAfter(parameters.getClaims().getIssuedAt());
    }

    @Test
    void generateTokenShouldRespectConfiguredExpirationWindow() {
        Jwt encodedJwt = Jwt.withTokenValue("signed-token")
                .headers(headers -> headers.putAll(Map.of("alg", "HS256")))
                .claims(claims -> claims.put("sub", "annotator@example.com"))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(1))
                .build();
        JwtTokenService shortTokenService = new JwtTokenService(jwtEncoder, 15L);
        when(jwtEncoder.encode(org.mockito.ArgumentMatchers.any(JwtEncoderParameters.class)))
                .thenReturn(encodedJwt);

        shortTokenService.generateToken("annotator@example.com");

        ArgumentCaptor<JwtEncoderParameters> captor = ArgumentCaptor.forClass(JwtEncoderParameters.class);
        verify(jwtEncoder).encode(captor.capture());
        long diff = captor.getValue().getClaims().getExpiresAt().getEpochSecond()
                - captor.getValue().getClaims().getIssuedAt().getEpochSecond();
        assertThat(diff).isEqualTo(15L);
    }
}
