package es.udc.fic.corpuslab.modules.auth.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OAuthExchangeRequestDto(
        @NotBlank @Size(min = 32, max = 255) String code
) {
}
