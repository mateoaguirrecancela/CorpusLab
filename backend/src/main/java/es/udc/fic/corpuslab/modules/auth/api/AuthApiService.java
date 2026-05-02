package es.udc.fic.corpuslab.modules.auth.api;

import java.util.Collection;
import java.util.List;

import es.udc.fic.corpuslab.modules.auth.api.dtos.UserInfo;

/**
 * Public API contract for the auth module.
 * Other modules MUST use this interface instead of importing
 * User entity or UserRepository directly.
 */
public interface AuthApiService {

    /**
     * Finds a user by email (case-insensitive, Google-canonicalized).
     *
     * @throws es.udc.fic.corpuslab.modules.auth.exceptions.EmailNotFoundException if not found
     */
    UserInfo findUserByEmail(String email);

    /**
     * Finds a user by ID.
     *
     * @throws es.udc.fic.corpuslab.modules.auth.exceptions.EmailNotFoundException if not found
     */
    UserInfo findUserById(Long userId);

    /**
     * Finds a user by email, returning Optional instead of throwing.
     */
    java.util.Optional<UserInfo> findUserByEmailOptional(String email);

    /**
     * Finds multiple users by their IDs.
     * Returns only the users that exist — does NOT throw if some are missing.
     */
    List<UserInfo> findUsersByIds(Collection<Long> userIds);
}
