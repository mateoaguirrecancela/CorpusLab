package es.udc.fic.corpuslab.modules.auth.dtos;

import java.time.Instant;
import java.time.LocalDate;

import es.udc.fic.corpuslab.modules.auth.enums.GenderType;

public record UserLoginResponseDto(
        Long id,
        String email,
        String firstName,
        String lastName,
        LocalDate birth,
        GenderType gender,
        String countryCode,
        String city,
        Instant createdAt,
        String token) {
}
