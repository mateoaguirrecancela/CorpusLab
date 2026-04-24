package es.udc.fic.corpuslab.common.security;

import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.http.HttpStatus;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    @Order(1)
    SecurityFilterChain oauthSecurityFilterChain(
            HttpSecurity http,
            ObjectProvider<org.springframework.security.oauth2.client.registration.ClientRegistrationRepository> clientRegistrationRepositoryProvider,
            ObjectProvider<OAuth2LoginSuccessHandler> oAuth2LoginSuccessHandlerProvider,
            ObjectProvider<OAuth2LoginFailureHandler> oAuth2LoginFailureHandlerProvider) {
        try {
            http.csrf(AbstractHttpConfigurer::disable)
                    .securityMatcher("/oauth2/**", "/login/oauth2/**")
                    .authorizeHttpRequests(auth -> auth
                            .anyRequest().permitAll())
                    .httpBasic(AbstractHttpConfigurer::disable)
                    .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED));

            if (clientRegistrationRepositoryProvider.getIfAvailable() != null) {
                OAuth2LoginSuccessHandler successHandler = oAuth2LoginSuccessHandlerProvider.getIfAvailable();
                OAuth2LoginFailureHandler failureHandler = oAuth2LoginFailureHandlerProvider.getIfAvailable();

                if (successHandler != null && failureHandler != null) {
                    http.oauth2Login(oauth -> oauth
                            .successHandler(successHandler)
                            .failureHandler(failureHandler));
                }
            }

            return http.build();
        } catch (Exception ex) {
            throw new IllegalStateException("Could not configure OAuth security filter chain", ex);
        }
    }

    @Bean
    @Order(2)
    SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) {
        try {
            return http
                    .securityMatcher("/api/**")
                    .csrf(AbstractHttpConfigurer::disable)
                    .headers(headers -> headers
                            .contentTypeOptions(org.springframework.security.config.annotation.web.configurers.HeadersConfigurer.ContentTypeOptionsConfig::disable) // Default is enabled, but showing how to configure
                            .contentTypeOptions(org.springframework.security.config.Customizer.withDefaults())
                    )
                    .authorizeHttpRequests(auth -> auth
                            .requestMatchers(
                                    "/api/auth/signup",
                                    "/api/auth/login",
                                    "/api/auth/logout",
                                    "/api/auth/forgot-password",
                                    "/api/auth/reset-password")
                            .permitAll()
                            .anyRequest().authenticated())
                    .exceptionHandling(
                            ex -> ex.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.FORBIDDEN)))
                    .httpBasic(AbstractHttpConfigurer::disable)
                    .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {
                    }))
                    .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                    .build();
        } catch (Exception ex) {
            throw new IllegalStateException("Could not configure API security filter chain", ex);
        }
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    JwtEncoder jwtEncoder(@Value("${app.jwt.secret}") String jwtSecret) {
        SecretKey secretKey = new SecretKeySpec(jwtSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        return new NimbusJwtEncoder(new ImmutableSecret<>(secretKey));
    }

    @Bean
    JwtDecoder jwtDecoder(@Value("${app.jwt.secret}") String jwtSecret) {
        SecretKey secretKey = new SecretKeySpec(jwtSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        return NimbusJwtDecoder.withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }
}
