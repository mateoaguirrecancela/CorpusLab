package es.udc.fic.corpuslab.modules.auth.services;

import es.udc.fic.corpuslab.modules.auth.dtos.UserLoginRequestDto;
import es.udc.fic.corpuslab.modules.auth.dtos.UserLoginResponseDto;
import es.udc.fic.corpuslab.modules.auth.dtos.UserProfileResponseDto;
import es.udc.fic.corpuslab.modules.auth.dtos.UserLogoutResponseDto;
import es.udc.fic.corpuslab.modules.auth.dtos.UserRegisterRequestDto;
import es.udc.fic.corpuslab.modules.auth.dtos.UserUpdateProfileRequestDto;

public interface AuthService {

    UserLoginResponseDto signup(UserRegisterRequestDto request);

    UserLoginResponseDto login(UserLoginRequestDto request);

    UserProfileResponseDto getProfile(String authenticatedEmail);

    UserProfileResponseDto updateProfile(String authenticatedEmail, UserUpdateProfileRequestDto request);

    UserLogoutResponseDto logout();

    UserLoginResponseDto exchangeOAuthCode(String code);

    void requestPasswordReset(String email);

    void resetPassword(String token, String newPassword);
}
