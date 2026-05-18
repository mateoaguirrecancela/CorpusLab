package es.udc.fic.corpuslab.common.exceptions;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import es.udc.fic.corpuslab.modules.auth.exceptions.AuthRateLimitExceededException;

@RestControllerAdvice
public class GlobalExceptionHandler {

        private final MessageSource messageSource;

        public GlobalExceptionHandler(MessageSource messageSource) {
                this.messageSource = messageSource;
        }

        private ResponseEntity<ApiErrorResponse> buildErrorResponse(
                        HttpStatus status,
                        String messageKey,
                        Object[] args,
                        Map<String, String> details) {
                String translatedMessage = messageSource.getMessage(
                                messageKey,
                                args,
                                LocaleContextHolder.getLocale());

                ApiErrorResponse response = new ApiErrorResponse(
                                Instant.now(),
                                status.value(),
                                status.getReasonPhrase(),
                                translatedMessage,
                                details);

                return ResponseEntity.status(status).body(response);
        }

        private ResponseEntity<ApiErrorResponse> buildErrorResponse(HttpStatus status, String messageKey,
                        Object[] args) {
                return buildErrorResponse(status, messageKey, args, null);
        }

        @ExceptionHandler(RuntimeException.class)
        public ResponseEntity<ApiErrorResponse> handleTranslatableApiException(RuntimeException ex) {
                if (ex instanceof TranslatableApiException translatable) {
                        return buildErrorResponse(
                                        translatable.getHttpStatus(),
                                        translatable.getMessageKey(),
                                        translatable.getMessageArgs());
                }
                throw ex;
        }

        @ExceptionHandler(AuthRateLimitExceededException.class)
        public ResponseEntity<ApiErrorResponse> handleAuthRateLimitExceeded(AuthRateLimitExceededException ex) {
                HttpStatus status = HttpStatus.TOO_MANY_REQUESTS;
                String translatedMessage = messageSource.getMessage(
                                "auth.error.rate.limit.exceeded",
                                null,
                                LocaleContextHolder.getLocale());
                ApiErrorResponse response = new ApiErrorResponse(
                                Instant.now(),
                                status.value(),
                                status.getReasonPhrase(),
                                translatedMessage,
                                null);

                return ResponseEntity.status(status)
                                .header(HttpHeaders.RETRY_AFTER, String.valueOf(ex.getRetryAfterSeconds()))
                                .body(response);
        }

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
                Map<String, String> details = new LinkedHashMap<>();
                for (FieldError error : ex.getBindingResult().getFieldErrors()) {
                        details.put(error.getField(), error.getDefaultMessage());
                }

                return buildErrorResponse(HttpStatus.BAD_REQUEST, "common.error.validation", null, details);
        }

        @ExceptionHandler(AccessDeniedException.class)
        public ResponseEntity<ApiErrorResponse> handleAccessDenied(AccessDeniedException ex) {
                return buildErrorResponse(HttpStatus.FORBIDDEN, "common.error.access.denied", null);
        }
}
