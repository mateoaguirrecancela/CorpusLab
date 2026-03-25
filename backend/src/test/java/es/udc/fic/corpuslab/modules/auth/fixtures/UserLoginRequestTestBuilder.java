package es.udc.fic.corpuslab.modules.auth.fixtures;

import es.udc.fic.corpuslab.modules.auth.dtos.UserLoginRequestDto;

public class UserLoginRequestTestBuilder {

    private String email = "new.user@example.com";
    private String password = "strong-password";

    public static UserLoginRequestTestBuilder validRequest() {
        return new UserLoginRequestTestBuilder();
    }

    public UserLoginRequestTestBuilder withEmail(String email) {
        this.email = email;
        return this;
    }

    public UserLoginRequestTestBuilder withPassword(String password) {
        this.password = password;
        return this;
    }

    public UserLoginRequestDto build() {
        return new UserLoginRequestDto(email, password);
    }
}
