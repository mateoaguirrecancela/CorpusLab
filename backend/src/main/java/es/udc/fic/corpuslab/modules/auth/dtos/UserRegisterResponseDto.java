package es.udc.fic.corpuslab.modules.auth.dtos;

import java.time.Instant;

public record UserRegisterResponseDto(
        Long id,
        String email,
        String firstName,
        String lastName,
        Instant createdAt
) {
}
