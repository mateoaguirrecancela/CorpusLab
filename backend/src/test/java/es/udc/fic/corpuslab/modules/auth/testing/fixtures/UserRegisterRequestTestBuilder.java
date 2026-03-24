package es.udc.fic.corpuslab.modules.auth.testing.fixtures;

import java.time.LocalDate;

import es.udc.fic.corpuslab.modules.auth.dtos.UserRegisterRequestDto;
import es.udc.fic.corpuslab.modules.auth.enums.GenderType;

public class UserRegisterRequestTestBuilder {

    private String email = "new.user@example.com";
    private String firstName = "New";
    private String lastName = "User";
    private LocalDate birth = LocalDate.of(1997, 5, 20);
    private GenderType gender = GenderType.OTHER;
    private String countryCode = "ES";
    private String city = "A Coruna";
    private String password = "strong-password";

    public static UserRegisterRequestTestBuilder validRequest() {
        return new UserRegisterRequestTestBuilder();
    }

    public UserRegisterRequestTestBuilder withEmail(String email) {
        this.email = email;
        return this;
    }

    public UserRegisterRequestTestBuilder withFirstName(String firstName) {
        this.firstName = firstName;
        return this;
    }

    public UserRegisterRequestTestBuilder withLastName(String lastName) {
        this.lastName = lastName;
        return this;
    }

    public UserRegisterRequestTestBuilder withBirth(LocalDate birth) {
        this.birth = birth;
        return this;
    }

    public UserRegisterRequestTestBuilder withGender(GenderType gender) {
        this.gender = gender;
        return this;
    }

    public UserRegisterRequestTestBuilder withCountryCode(String countryCode) {
        this.countryCode = countryCode;
        return this;
    }

    public UserRegisterRequestTestBuilder withCity(String city) {
        this.city = city;
        return this;
    }

    public UserRegisterRequestTestBuilder withPassword(String password) {
        this.password = password;
        return this;
    }

    public UserRegisterRequestDto build() {
        return new UserRegisterRequestDto(
                email,
                firstName,
                lastName,
                birth,
                gender,
                countryCode,
                city,
                password
        );
    }
}
