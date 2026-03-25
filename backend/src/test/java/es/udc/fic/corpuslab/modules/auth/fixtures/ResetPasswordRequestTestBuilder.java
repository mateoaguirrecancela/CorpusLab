package es.udc.fic.corpuslab.modules.auth.fixtures;

import es.udc.fic.corpuslab.modules.auth.dtos.ResetPasswordRequestDto;

public class ResetPasswordRequestTestBuilder {

    private String token = "valid-token-example";
    private String newPassword = "new-strong-password";

    public static ResetPasswordRequestTestBuilder validRequest() {
        return new ResetPasswordRequestTestBuilder();
    }

    public ResetPasswordRequestTestBuilder withToken(String token) {
        this.token = token;
        return this;
    }

    public ResetPasswordRequestTestBuilder withNewPassword(String newPassword) {
        this.newPassword = newPassword;
        return this;
    }

    public ResetPasswordRequestDto build() {
        return new ResetPasswordRequestDto(token, newPassword);
    }
}
