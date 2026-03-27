package es.udc.fic.corpuslab.modules.auth.controllers;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import es.udc.fic.corpuslab.modules.auth.dtos.UserLoginRequestDto;
import es.udc.fic.corpuslab.modules.auth.dtos.UserLoginResponseDto;
import es.udc.fic.corpuslab.modules.auth.dtos.UserProfileResponseDto;
import es.udc.fic.corpuslab.modules.auth.dtos.UserUpdateProfileRequestDto;
import es.udc.fic.corpuslab.modules.auth.dtos.UserRegisterRequestDto;
import es.udc.fic.corpuslab.modules.auth.dtos.UserRegisterResponseDto;
import es.udc.fic.corpuslab.modules.auth.dtos.UserLogoutResponseDto;
import es.udc.fic.corpuslab.modules.auth.dtos.ForgotPasswordRequestDto;
import es.udc.fic.corpuslab.modules.auth.dtos.ForgotPasswordResponseDto;
import es.udc.fic.corpuslab.modules.auth.dtos.ResetPasswordRequestDto;
import es.udc.fic.corpuslab.modules.auth.dtos.ResetPasswordResponseDto;
import es.udc.fic.corpuslab.modules.auth.services.AuthService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public UserRegisterResponseDto signup(@Valid @RequestBody UserRegisterRequestDto request) {
        return authService.signup(request);
    }

    @PostMapping("/login")
    public UserLoginResponseDto login(@Valid @RequestBody UserLoginRequestDto request, HttpServletRequest httpRequest) {
        return authService.login(request, httpRequest);
    }

    @GetMapping("/profile")
    public UserProfileResponseDto profile(Authentication authentication) {
        return authService.getProfile(authentication.getName());
    }

    @PutMapping("/profile")
    public UserProfileResponseDto updateProfile(
            Authentication authentication,
            @Valid @RequestBody UserUpdateProfileRequestDto request
    ) {
        return authService.updateProfile(authentication.getName(), request);
    }

    @PostMapping("/logout")
    public UserLogoutResponseDto logout(HttpServletRequest httpRequest) {
        return authService.logout(httpRequest);
    }

    @PostMapping("/forgot-password")
    public ForgotPasswordResponseDto forgotPassword(@Valid @RequestBody ForgotPasswordRequestDto request) {
        authService.requestPasswordReset(request.email());
        return new ForgotPasswordResponseDto("If the account exists, a reset link has been sent");
    }

    @PostMapping("/reset-password")
    public ResetPasswordResponseDto resetPassword(@Valid @RequestBody ResetPasswordRequestDto request) {
        authService.resetPassword(request.token(), request.newPassword());
        return new ResetPasswordResponseDto("If the token is valid, password has been reset");
    }
}
