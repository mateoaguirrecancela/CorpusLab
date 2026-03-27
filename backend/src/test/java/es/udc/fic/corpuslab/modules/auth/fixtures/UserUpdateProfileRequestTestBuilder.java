package es.udc.fic.corpuslab.modules.auth.fixtures;

import java.time.LocalDate;

import es.udc.fic.corpuslab.modules.auth.dtos.UserUpdateProfileRequestDto;
import es.udc.fic.corpuslab.modules.auth.enums.GenderType;

public class UserUpdateProfileRequestTestBuilder {

    private String firstName = "Updated";
    private String lastName = "Profile";
    private LocalDate birth = LocalDate.of(1999, 6, 15);
    private GenderType gender = GenderType.FEMALE;
    private String countryCode = "PT";
    private String city = "Porto";

    public static UserUpdateProfileRequestTestBuilder validRequest() {
        return new UserUpdateProfileRequestTestBuilder();
    }

    public UserUpdateProfileRequestTestBuilder withFirstName(String firstName) {
        this.firstName = firstName;
        return this;
    }

    public UserUpdateProfileRequestTestBuilder withLastName(String lastName) {
        this.lastName = lastName;
        return this;
    }

    public UserUpdateProfileRequestTestBuilder withBirth(LocalDate birth) {
        this.birth = birth;
        return this;
    }

    public UserUpdateProfileRequestTestBuilder withGender(GenderType gender) {
        this.gender = gender;
        return this;
    }

    public UserUpdateProfileRequestTestBuilder withCountryCode(String countryCode) {
        this.countryCode = countryCode;
        return this;
    }

    public UserUpdateProfileRequestTestBuilder withCity(String city) {
        this.city = city;
        return this;
    }

    public UserUpdateProfileRequestDto build() {
        return new UserUpdateProfileRequestDto(
                firstName,
                lastName,
                birth,
                gender,
                countryCode,
                city
        );
    }
}
