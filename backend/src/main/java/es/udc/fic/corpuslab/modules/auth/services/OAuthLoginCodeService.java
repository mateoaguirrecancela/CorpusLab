package es.udc.fic.corpuslab.modules.auth.services;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.udc.fic.corpuslab.modules.auth.entities.OAuthLoginCode;
import es.udc.fic.corpuslab.modules.auth.entities.User;
import es.udc.fic.corpuslab.modules.auth.exceptions.OAuthLoginCodeNotFoundException;
import es.udc.fic.corpuslab.modules.auth.repositories.OAuthLoginCodeRepository;
import es.udc.fic.corpuslab.modules.auth.utils.SecureTokenUtils;

@Service
public class OAuthLoginCodeService {

    private static final int CODE_BYTE_LENGTH = 32;

    private final OAuthLoginCodeRepository oauthLoginCodeRepository;

    public OAuthLoginCodeService(OAuthLoginCodeRepository oauthLoginCodeRepository) {
        this.oauthLoginCodeRepository = oauthLoginCodeRepository;
    }

    @Transactional
    public String createCode(User user, String provider) {
        oauthLoginCodeRepository.deleteByExpiresAtBeforeOrConsumedAtIsNotNull(Instant.now());

        String rawCode = SecureTokenUtils.randomUrlSafeToken(CODE_BYTE_LENGTH);
        OAuthLoginCode loginCode = new OAuthLoginCode();
        loginCode.setCodeHash(SecureTokenUtils.sha256(rawCode));
        loginCode.setProvider(provider);
        loginCode.setUser(user);
        loginCode.setExpiresAt(Instant.now().plus(2, ChronoUnit.MINUTES));
        oauthLoginCodeRepository.save(loginCode);

        return rawCode;
    }

    @Transactional
    public User consumeCode(String rawCode) {
        if (rawCode == null || rawCode.isBlank()) {
            throw new OAuthLoginCodeNotFoundException();
        }

        Instant now = Instant.now();
        String codeHash = SecureTokenUtils.sha256(rawCode.trim());
        int consumedCodes = oauthLoginCodeRepository.markAsConsumedIfValid(codeHash, now);
        if (consumedCodes != 1) {
            throw new OAuthLoginCodeNotFoundException();
        }

        OAuthLoginCode loginCode = oauthLoginCodeRepository.findByCodeHash(codeHash)
                .orElseThrow(OAuthLoginCodeNotFoundException::new);
        return loginCode.getUser();
    }
}
