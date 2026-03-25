package es.udc.fic.corpuslab.modules.auth.fixtures;

import es.udc.fic.corpuslab.modules.auth.dtos.ForgotPasswordRequestDto;

public class ForgotPasswordRequestTestBuilder {

    private String email = "new.user@example.com";

    public static ForgotPasswordRequestTestBuilder validRequest() {
        return new ForgotPasswordRequestTestBuilder();
    }

    public ForgotPasswordRequestTestBuilder withEmail(String email) {
        this.email = email;
        return this;
    }

    public ForgotPasswordRequestDto build() {
        return new ForgotPasswordRequestDto(email);
    }
}
