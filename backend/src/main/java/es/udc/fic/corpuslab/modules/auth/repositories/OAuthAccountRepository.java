package es.udc.fic.corpuslab.modules.auth.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import es.udc.fic.corpuslab.modules.auth.entities.OAuthAccount;

public interface OAuthAccountRepository extends JpaRepository<OAuthAccount, Long> {

    Optional<OAuthAccount> findByProviderAndProviderUserId(String provider, String providerUserId);
}
