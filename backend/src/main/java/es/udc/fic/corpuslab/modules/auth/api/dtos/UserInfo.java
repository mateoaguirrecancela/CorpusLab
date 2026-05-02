package es.udc.fic.corpuslab.modules.auth.api.dtos;

public record UserInfo(
        Long userId,
        String email,
        String firstName,
        String lastName) {

    public String fullName() {
        return (firstName + " " + lastName).trim();
    }
}
