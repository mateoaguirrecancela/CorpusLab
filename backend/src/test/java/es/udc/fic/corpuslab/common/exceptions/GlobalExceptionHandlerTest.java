package es.udc.fic.corpuslab.common.exceptions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import es.udc.fic.corpuslab.modules.auth.exceptions.AuthRateLimitExceededException;
import es.udc.fic.corpuslab.modules.project.shared.exceptions.ProjectNotFoundException;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @Mock
    private MessageSource messageSource;

    @Test
    void handleTranslatableApiExceptionShouldUseExceptionStatusKeyAndArgs() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler(messageSource);
        when(messageSource.getMessage(eq("project.error.notfound"), any(Object[].class), any(Locale.class)))
                .thenReturn("Project 42 not found");

        ResponseEntity<ApiErrorResponse> response =
                handler.handleTranslatableApiException(new ProjectNotFoundException(42L));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(response.getBody().message()).isEqualTo("Project 42 not found");
        verify(messageSource).getMessage(eq("project.error.notfound"), aryEq(new Object[] { 42L }), any(Locale.class));
    }

    @Test
    void handleAuthRateLimitExceededShouldKeepRetryAfterHeader() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler(messageSource);
        when(messageSource.getMessage(eq("auth.error.rate.limit.exceeded"), isNull(), any(Locale.class)))
                .thenReturn("Too many requests");

        ResponseEntity<ApiErrorResponse> response =
                handler.handleAuthRateLimitExceeded(new AuthRateLimitExceededException(15L));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(response.getHeaders().getFirst(HttpHeaders.RETRY_AFTER)).isEqualTo("15");
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Too many requests");
    }

    @Test
    void handleUnhandledExceptionShouldReturnInternalServerErrorWithUuid() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler(messageSource);
        when(messageSource.getMessage(eq("common.error.internal.server.error"), any(Object[].class), any(Locale.class)))
                .thenReturn("Internal error with reference ID: 12345");

        ResponseEntity<ApiErrorResponse> response =
                handler.handleUnhandledException(new NullPointerException("NPE test"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(response.getBody().message()).isEqualTo("Internal error with reference ID: 12345");
        assertThat(response.getBody().details()).containsKey("errorId");
    }

    @Test
    void handleTranslatableApiExceptionWithUnhandledRuntimeExceptionShouldDelegateToUnhandledException() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler(messageSource);
        when(messageSource.getMessage(eq("common.error.internal.server.error"), any(Object[].class), any(Locale.class)))
                .thenReturn("Internal error");

        ResponseEntity<ApiErrorResponse> response =
                handler.handleTranslatableApiException(new IllegalArgumentException("Illegal arg test"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().details()).containsKey("errorId");
    }
}

