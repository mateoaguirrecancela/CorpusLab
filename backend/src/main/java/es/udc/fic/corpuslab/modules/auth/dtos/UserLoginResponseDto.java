package es.udc.fic.corpuslab.modules.auth.dtos;

public record UserLoginResponseDto(
        Long id,
        String email,
        String firstName,
        String lastName
) {
}
