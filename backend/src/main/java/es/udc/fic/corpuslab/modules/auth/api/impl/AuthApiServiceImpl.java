package es.udc.fic.corpuslab.modules.auth.api.impl;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.udc.fic.corpuslab.modules.auth.api.AuthApiService;
import es.udc.fic.corpuslab.modules.auth.api.dtos.UserInfo;
import es.udc.fic.corpuslab.modules.auth.entities.User;
import es.udc.fic.corpuslab.modules.auth.exceptions.EmailNotFoundException;
import es.udc.fic.corpuslab.modules.auth.repositories.UserRepository;
import es.udc.fic.corpuslab.modules.auth.utils.EmailNormalizer;

@Service
@Transactional(readOnly = true)
public class AuthApiServiceImpl implements AuthApiService {

    private final UserRepository userRepository;

    public AuthApiServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserInfo findUserByEmail(String email) {
        String normalizedEmail = EmailNormalizer.canonicalizeGoogleEmail(email);
        User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new EmailNotFoundException(normalizedEmail));
        return toUserInfo(user);
    }

    @Override
    public UserInfo findUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EmailNotFoundException("User not found with id: " + userId));
        return toUserInfo(user);
    }

    @Override
    public Optional<UserInfo> findUserByEmailOptional(String email) {
        String normalizedEmail = EmailNormalizer.canonicalizeGoogleEmail(email);
        return userRepository.findByEmailIgnoreCase(normalizedEmail)
                .map(this::toUserInfo);
    }

    @Override
    public List<UserInfo> findUsersByIds(Collection<Long> userIds) {
        return userRepository.findAllById(userIds).stream()
                .map(this::toUserInfo)
                .toList();
    }

    private UserInfo toUserInfo(User user) {
        return new UserInfo(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName());
    }
}
