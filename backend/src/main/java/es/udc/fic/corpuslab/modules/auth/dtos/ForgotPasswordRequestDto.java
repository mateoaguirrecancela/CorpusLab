package es.udc.fic.corpuslab.modules.auth.dtos;

import jakarta.validation.constraints.Email;

public record ForgotPasswordRequestDto(
        @Email String email
) {
}
