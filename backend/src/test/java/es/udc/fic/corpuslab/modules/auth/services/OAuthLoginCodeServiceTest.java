package es.udc.fic.corpuslab.modules.auth.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import es.udc.fic.corpuslab.modules.auth.entities.OAuthLoginCode;
import es.udc.fic.corpuslab.modules.auth.entities.User;
import es.udc.fic.corpuslab.modules.auth.exceptions.OAuthLoginCodeNotFoundException;
import es.udc.fic.corpuslab.modules.auth.fixtures.UserTestBuilder;
import es.udc.fic.corpuslab.modules.auth.repositories.OAuthLoginCodeRepository;
import es.udc.fic.corpuslab.modules.auth.utils.SecureTokenUtils;

@ExtendWith(MockitoExtension.class)
class OAuthLoginCodeServiceTest {

    @Mock
    private OAuthLoginCodeRepository oauthLoginCodeRepository;

    @Test
    void consumeCodeShouldAtomicallyMarkValidCodeAsConsumedAndReturnUser() {
        OAuthLoginCodeService service = new OAuthLoginCodeService(oauthLoginCodeRepository);
        String rawCode = "valid-one-time-oauth-code";
        String codeHash = SecureTokenUtils.sha256(rawCode);
        User user = UserTestBuilder.validUser().build();
        OAuthLoginCode loginCode = new OAuthLoginCode();
        loginCode.setCodeHash(codeHash);
        loginCode.setProvider("google");
        loginCode.setUser(user);
        loginCode.setExpiresAt(Instant.now().plusSeconds(120));

        when(oauthLoginCodeRepository.markAsConsumedIfValid(eq(codeHash), any(Instant.class))).thenReturn(1);
        when(oauthLoginCodeRepository.findByCodeHash(codeHash)).thenReturn(Optional.of(loginCode));

        User consumedUser = service.consumeCode(rawCode);

        assertThat(consumedUser).isSameAs(user);
        verify(oauthLoginCodeRepository).markAsConsumedIfValid(eq(codeHash), any(Instant.class));
        verify(oauthLoginCodeRepository, never()).save(any(OAuthLoginCode.class));
    }

    @Test
    void consumeCodeShouldFailWhenConditionalUpdateDoesNotConsumeExactlyOneCode() {
        OAuthLoginCodeService service = new OAuthLoginCodeService(oauthLoginCodeRepository);
        String rawCode = "already-consumed-oauth-code";
        String codeHash = SecureTokenUtils.sha256(rawCode);

        when(oauthLoginCodeRepository.markAsConsumedIfValid(eq(codeHash), any(Instant.class))).thenReturn(0);

        assertThatThrownBy(() -> service.consumeCode(rawCode))
                .isInstanceOf(OAuthLoginCodeNotFoundException.class);

        verify(oauthLoginCodeRepository, never()).findByCodeHash(any());
        verify(oauthLoginCodeRepository, never()).save(any(OAuthLoginCode.class));
    }
}
