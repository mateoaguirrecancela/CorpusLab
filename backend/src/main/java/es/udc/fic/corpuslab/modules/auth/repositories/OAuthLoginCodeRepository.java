package es.udc.fic.corpuslab.modules.auth.repositories;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import es.udc.fic.corpuslab.modules.auth.entities.OAuthLoginCode;

public interface OAuthLoginCodeRepository extends JpaRepository<OAuthLoginCode, Long> {

    Optional<OAuthLoginCode> findByCodeHash(String codeHash);

    long deleteByExpiresAtBeforeOrConsumedAtIsNotNull(Instant now);
}
