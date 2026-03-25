package es.udc.fic.corpuslab.modules.auth.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import es.udc.fic.corpuslab.modules.auth.entities.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByEmailIgnoreCase(String email);
    Optional<User> findByEmailIgnoreCase(String email);
}
