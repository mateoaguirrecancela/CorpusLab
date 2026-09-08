package es.udc.fic.corpuslab.modules.auth.repositories;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import es.udc.fic.corpuslab.modules.auth.entities.OAuthLoginCode;

public interface OAuthLoginCodeRepository extends JpaRepository<OAuthLoginCode, Long> {

    Optional<OAuthLoginCode> findByCodeHash(String codeHash);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update OAuthLoginCode code
            set code.consumedAt = :consumedAt
            where code.codeHash = :codeHash
              and code.consumedAt is null
              and code.expiresAt > :consumedAt
            """)
    int markAsConsumedIfValid(
            @Param("codeHash") String codeHash,
            @Param("consumedAt") Instant consumedAt);

    long deleteByExpiresAtBeforeOrConsumedAtIsNotNull(Instant now);
}
