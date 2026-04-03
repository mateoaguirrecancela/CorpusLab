package es.udc.fic.corpuslab.common.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import es.udc.fic.corpuslab.modules.auth.exceptions.EmailAlreadyRegisteredException;
import es.udc.fic.corpuslab.modules.auth.exceptions.EmailNotFoundException;
import es.udc.fic.corpuslab.modules.auth.exceptions.InvalidCredentialsException;
import es.udc.fic.corpuslab.modules.auth.exceptions.PasswordResetEmailDeliveryException;
import es.udc.fic.corpuslab.modules.auth.exceptions.PasswordResetTokenNotFoundException;
import es.udc.fic.corpuslab.modules.researchgroup.exceptions.InvalidResearchGroupInvitationRoleException;
import es.udc.fic.corpuslab.modules.researchgroup.exceptions.InvalidResearchGroupMemberRoleException;
import es.udc.fic.corpuslab.modules.researchgroup.exceptions.ResearchGroupInvitationAlreadyExistsException;
import es.udc.fic.corpuslab.modules.researchgroup.exceptions.ResearchGroupInvitationCodeNotFoundException;
import es.udc.fic.corpuslab.modules.researchgroup.exceptions.ResearchGroupInvitationEmailDeliveryException;
import es.udc.fic.corpuslab.modules.researchgroup.exceptions.ResearchGroupInvitationNotFoundException;
import es.udc.fic.corpuslab.modules.researchgroup.exceptions.ResearchGroupMemberAlreadyExistsException;
import es.udc.fic.corpuslab.modules.researchgroup.exceptions.ResearchGroupMemberNotFoundException;
import es.udc.fic.corpuslab.modules.researchgroup.exceptions.ResearchGroupNotFoundException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

        private final MessageSource messageSource;

        public GlobalExceptionHandler(MessageSource messageSource) {
                this.messageSource = messageSource;
        }

        @ExceptionHandler(EmailAlreadyRegisteredException.class)
        public ResponseEntity<ApiErrorResponse> handleEmailAlreadyRegistered(EmailAlreadyRegisteredException ex) {
                String translatedMessage = messageSource.getMessage(
                                "auth.error.email.exists",
                                new Object[] { ex.getEmail() },
                                LocaleContextHolder.getLocale());

                ApiErrorResponse response = new ApiErrorResponse(
                                Instant.now(),
                                HttpStatus.CONFLICT.value(),
                                HttpStatus.CONFLICT.getReasonPhrase(),
                                translatedMessage,
                                null);
                return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }

        @ExceptionHandler(InvalidCredentialsException.class)
        public ResponseEntity<ApiErrorResponse> handleInvalidCredentials(InvalidCredentialsException ex) {
                String translatedMessage = messageSource.getMessage(
                                "auth.error.invalid.credentials",
                                null,
                                LocaleContextHolder.getLocale());

                ApiErrorResponse response = new ApiErrorResponse(
                                Instant.now(),
                                HttpStatus.UNAUTHORIZED.value(),
                                HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                                translatedMessage,
                                null);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        @ExceptionHandler(PasswordResetEmailDeliveryException.class)
        public ResponseEntity<ApiErrorResponse> handlePasswordResetEmailDelivery(
                        PasswordResetEmailDeliveryException ex) {
                String translatedMessage = messageSource.getMessage(
                                "auth.error.reset.email.delivery",
                                null,
                                LocaleContextHolder.getLocale());

                ApiErrorResponse response = new ApiErrorResponse(
                                Instant.now(),
                                HttpStatus.SERVICE_UNAVAILABLE.value(),
                                HttpStatus.SERVICE_UNAVAILABLE.getReasonPhrase(),
                                translatedMessage,
                                null);
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
        }

        @ExceptionHandler(EmailNotFoundException.class)
        public ResponseEntity<ApiErrorResponse> handleEmailNotFound(EmailNotFoundException ex) {
                String translatedMessage = messageSource.getMessage(
                                "auth.error.email.notfound",
                                new Object[] { ex.getEmail() },
                                LocaleContextHolder.getLocale());

                ApiErrorResponse response = new ApiErrorResponse(
                                Instant.now(),
                                HttpStatus.NOT_FOUND.value(),
                                HttpStatus.NOT_FOUND.getReasonPhrase(),
                                translatedMessage,
                                null);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        @ExceptionHandler(PasswordResetTokenNotFoundException.class)
        public ResponseEntity<ApiErrorResponse> handlePasswordResetTokenNotFound(
                        PasswordResetTokenNotFoundException ex) {
                String translatedMessage = messageSource.getMessage(
                                "auth.error.reset.token.notfound",
                                null,
                                LocaleContextHolder.getLocale());

                ApiErrorResponse response = new ApiErrorResponse(
                                Instant.now(),
                                HttpStatus.NOT_FOUND.value(),
                                HttpStatus.NOT_FOUND.getReasonPhrase(),
                                translatedMessage,
                                null);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        @ExceptionHandler(ResearchGroupNotFoundException.class)
        public ResponseEntity<ApiErrorResponse> handleResearchGroupNotFound(ResearchGroupNotFoundException ex) {
                String translatedMessage = messageSource.getMessage(
                                "researchgroup.error.notfound",
                                new Object[] { ex.getId() },
                                LocaleContextHolder.getLocale());

                ApiErrorResponse response = new ApiErrorResponse(
                                Instant.now(),
                                HttpStatus.NOT_FOUND.value(),
                                HttpStatus.NOT_FOUND.getReasonPhrase(),
                                translatedMessage,
                                null);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        @ExceptionHandler(ResearchGroupInvitationAlreadyExistsException.class)
        public ResponseEntity<ApiErrorResponse> handleResearchGroupInvitationAlreadyExists(
                        ResearchGroupInvitationAlreadyExistsException ex) {
                String translatedMessage = messageSource.getMessage(
                                "researchgroup.invitation.error.already.exists",
                                new Object[] { ex.getEmail(), ex.getGroupId() },
                                LocaleContextHolder.getLocale());

                ApiErrorResponse response = new ApiErrorResponse(
                                Instant.now(),
                                HttpStatus.CONFLICT.value(),
                                HttpStatus.CONFLICT.getReasonPhrase(),
                                translatedMessage,
                                null);
                return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }

        @ExceptionHandler(ResearchGroupMemberAlreadyExistsException.class)
        public ResponseEntity<ApiErrorResponse> handleResearchGroupMemberAlreadyExists(
                        ResearchGroupMemberAlreadyExistsException ex) {
                String translatedMessage = messageSource.getMessage(
                                "researchgroup.invitation.error.already.member",
                                new Object[] { ex.getEmail(), ex.getGroupId() },
                                LocaleContextHolder.getLocale());

                ApiErrorResponse response = new ApiErrorResponse(
                                Instant.now(),
                                HttpStatus.CONFLICT.value(),
                                HttpStatus.CONFLICT.getReasonPhrase(),
                                translatedMessage,
                                null);
                return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }

        @ExceptionHandler(ResearchGroupInvitationEmailDeliveryException.class)
        public ResponseEntity<ApiErrorResponse> handleResearchGroupInvitationEmailDelivery(
                        ResearchGroupInvitationEmailDeliveryException ex) {
                String translatedMessage = messageSource.getMessage(
                                "researchgroup.invitation.error.email.delivery",
                                null,
                                LocaleContextHolder.getLocale());

                ApiErrorResponse response = new ApiErrorResponse(
                                Instant.now(),
                                HttpStatus.SERVICE_UNAVAILABLE.value(),
                                HttpStatus.SERVICE_UNAVAILABLE.getReasonPhrase(),
                                translatedMessage,
                                null);
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
        }

        @ExceptionHandler(ResearchGroupInvitationCodeNotFoundException.class)
        public ResponseEntity<ApiErrorResponse> handleResearchGroupInvitationCodeNotFound(
                        ResearchGroupInvitationCodeNotFoundException ex) {
                String translatedMessage = messageSource.getMessage(
                                "researchgroup.invitation.code.notfound",
                                new Object[] { ex.getCode() },
                                LocaleContextHolder.getLocale());

                ApiErrorResponse response = new ApiErrorResponse(
                                Instant.now(),
                                HttpStatus.NOT_FOUND.value(),
                                HttpStatus.NOT_FOUND.getReasonPhrase(),
                                translatedMessage,
                                null);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        @ExceptionHandler(ResearchGroupInvitationNotFoundException.class)
        public ResponseEntity<ApiErrorResponse> handleResearchGroupInvitationNotFound(
                        ResearchGroupInvitationNotFoundException ex) {
                String translatedMessage = messageSource.getMessage(
                                "researchgroup.invitation.notfound",
                                new Object[] { ex.getInvitationId() },
                                LocaleContextHolder.getLocale());

                ApiErrorResponse response = new ApiErrorResponse(
                                Instant.now(),
                                HttpStatus.NOT_FOUND.value(),
                                HttpStatus.NOT_FOUND.getReasonPhrase(),
                                translatedMessage,
                                null);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        @ExceptionHandler(InvalidResearchGroupInvitationRoleException.class)
        public ResponseEntity<ApiErrorResponse> handleInvalidResearchGroupInvitationRole(
                        InvalidResearchGroupInvitationRoleException ex) {
                String translatedMessage = messageSource.getMessage(
                                "researchgroup.invitation.role.invalid",
                                new Object[] { ex.getRole().name() },
                                LocaleContextHolder.getLocale());

                ApiErrorResponse response = new ApiErrorResponse(
                                Instant.now(),
                                HttpStatus.BAD_REQUEST.value(),
                                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                                translatedMessage,
                                null);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        @ExceptionHandler(InvalidResearchGroupMemberRoleException.class)
        public ResponseEntity<ApiErrorResponse> handleInvalidResearchGroupMemberRole(
                        InvalidResearchGroupMemberRoleException ex) {
                String translatedMessage = messageSource.getMessage(
                                "researchgroup.member.role.invalid",
                                new Object[] { ex.getRole().name() },
                                LocaleContextHolder.getLocale());

                ApiErrorResponse response = new ApiErrorResponse(
                                Instant.now(),
                                HttpStatus.BAD_REQUEST.value(),
                                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                                translatedMessage,
                                null);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        @ExceptionHandler(ResearchGroupMemberNotFoundException.class)
        public ResponseEntity<ApiErrorResponse> handleResearchGroupMemberNotFound(
                        ResearchGroupMemberNotFoundException ex) {
                String translatedMessage = messageSource.getMessage(
                                "researchgroup.member.notfound",
                                new Object[] { ex.getGroupId(), ex.getUserId() },
                                LocaleContextHolder.getLocale());

                ApiErrorResponse response = new ApiErrorResponse(
                                Instant.now(),
                                HttpStatus.NOT_FOUND.value(),
                                HttpStatus.NOT_FOUND.getReasonPhrase(),
                                translatedMessage,
                                null);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
                Map<String, String> details = new LinkedHashMap<>();
                for (FieldError error : ex.getBindingResult().getFieldErrors()) {
                        details.put(error.getField(), error.getDefaultMessage());
                }

                ApiErrorResponse response = new ApiErrorResponse(
                                Instant.now(),
                                HttpStatus.BAD_REQUEST.value(),
                                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                                messageSource.getMessage("common.error.validation", null,
                                                LocaleContextHolder.getLocale()),
                                details);
                return ResponseEntity.badRequest().body(response);
        }
}
