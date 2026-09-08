package es.udc.fic.corpuslab.modules.auth.fixtures;

import java.time.LocalDate;

import es.udc.fic.corpuslab.modules.auth.entities.User;
import es.udc.fic.corpuslab.modules.auth.enums.GenderType;

public class UserTestBuilder {

    private String email = "new.user@example.com";
    private String firstName = "New";
    private String lastName = "User";
    private LocalDate birth = LocalDate.of(1997, 5, 20);
    private GenderType gender = GenderType.OTHER;
    private String countryCode = "ES";
    private String city = "A Coruna";
    private String passwordHash = "$2a$10$abcdefghijklmnopqrstuv123456789012345678901234567890";

    public static UserTestBuilder validUser() {
        return new UserTestBuilder();
    }

    public UserTestBuilder withEmail(String email) {
        this.email = email;
        return this;
    }

    public UserTestBuilder withFirstName(String firstName) {
        this.firstName = firstName;
        return this;
    }

    public UserTestBuilder withLastName(String lastName) {
        this.lastName = lastName;
        return this;
    }

    public UserTestBuilder withBirth(LocalDate birth) {
        this.birth = birth;
        return this;
    }

    public UserTestBuilder withGender(GenderType gender) {
        this.gender = gender;
        return this;
    }

    public UserTestBuilder withCountryCode(String countryCode) {
        this.countryCode = countryCode;
        return this;
    }

    public UserTestBuilder withCity(String city) {
        this.city = city;
        return this;
    }

    public UserTestBuilder withPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
        return this;
    }

    public User build() {
        User user = new User();
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setBirth(birth);
        user.setGender(gender);
        user.setCountryCode(countryCode);
        user.setCity(city);
        user.setPasswordHash(passwordHash);
        return user;
    }
}
