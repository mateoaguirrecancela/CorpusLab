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
import es.udc.fic.corpuslab.modules.notification.exceptions.NotificationNotFoundException;
import es.udc.fic.corpuslab.modules.project.exceptions.InvalidProjectDatasetException;
import es.udc.fic.corpuslab.modules.project.exceptions.InvalidProjectSetupException;
import es.udc.fic.corpuslab.modules.project.exceptions.ProjectNotFoundException;
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

        @ExceptionHandler(EmailAlreadyRegisteredException.class)
        public ResponseEntity<ApiErrorResponse> handleEmailAlreadyRegistered(EmailAlreadyRegisteredException ex) {
                return buildErrorResponse(HttpStatus.CONFLICT, "auth.error.email.exists",
                                new Object[] { ex.getEmail() });
        }

        @ExceptionHandler(InvalidCredentialsException.class)
        public ResponseEntity<ApiErrorResponse> handleInvalidCredentials(InvalidCredentialsException ex) {
                return buildErrorResponse(HttpStatus.UNAUTHORIZED, "auth.error.invalid.credentials", null);
        }

        @ExceptionHandler(PasswordResetEmailDeliveryException.class)
        public ResponseEntity<ApiErrorResponse> handlePasswordResetEmailDelivery(
                        PasswordResetEmailDeliveryException ex) {
                return buildErrorResponse(HttpStatus.SERVICE_UNAVAILABLE, "auth.error.reset.email.delivery", null);
        }

        @ExceptionHandler(EmailNotFoundException.class)
        public ResponseEntity<ApiErrorResponse> handleEmailNotFound(EmailNotFoundException ex) {
                return buildErrorResponse(HttpStatus.NOT_FOUND, "auth.error.email.notfound",
                                new Object[] { ex.getEmail() });
        }

        @ExceptionHandler(PasswordResetTokenNotFoundException.class)
        public ResponseEntity<ApiErrorResponse> handlePasswordResetTokenNotFound(
                        PasswordResetTokenNotFoundException ex) {
                return buildErrorResponse(HttpStatus.NOT_FOUND, "auth.error.reset.token.notfound", null);
        }

        @ExceptionHandler(ResearchGroupNotFoundException.class)
        public ResponseEntity<ApiErrorResponse> handleResearchGroupNotFound(ResearchGroupNotFoundException ex) {
                return buildErrorResponse(HttpStatus.NOT_FOUND, "researchgroup.error.notfound",
                                new Object[] { ex.getId() });
        }

        @ExceptionHandler(ResearchGroupInvitationAlreadyExistsException.class)
        public ResponseEntity<ApiErrorResponse> handleResearchGroupInvitationAlreadyExists(
                        ResearchGroupInvitationAlreadyExistsException ex) {
                return buildErrorResponse(
                                HttpStatus.CONFLICT,
                                "researchgroup.invitation.error.already.exists",
                                new Object[] { ex.getEmail(), ex.getGroupId() });
        }

        @ExceptionHandler(ResearchGroupMemberAlreadyExistsException.class)
        public ResponseEntity<ApiErrorResponse> handleResearchGroupMemberAlreadyExists(
                        ResearchGroupMemberAlreadyExistsException ex) {
                return buildErrorResponse(
                                HttpStatus.CONFLICT,
                                "researchgroup.invitation.error.already.member",
                                new Object[] { ex.getEmail(), ex.getGroupId() });
        }

        @ExceptionHandler(ResearchGroupInvitationEmailDeliveryException.class)
        public ResponseEntity<ApiErrorResponse> handleResearchGroupInvitationEmailDelivery(
                        ResearchGroupInvitationEmailDeliveryException ex) {
                return buildErrorResponse(HttpStatus.SERVICE_UNAVAILABLE,
                                "researchgroup.invitation.error.email.delivery", null);
        }

        @ExceptionHandler(ResearchGroupInvitationCodeNotFoundException.class)
        public ResponseEntity<ApiErrorResponse> handleResearchGroupInvitationCodeNotFound(
                        ResearchGroupInvitationCodeNotFoundException ex) {
                return buildErrorResponse(
                                HttpStatus.NOT_FOUND,
                                "researchgroup.invitation.code.notfound",
                                new Object[] { ex.getCode() });
        }

        @ExceptionHandler(ResearchGroupInvitationNotFoundException.class)
        public ResponseEntity<ApiErrorResponse> handleResearchGroupInvitationNotFound(
                        ResearchGroupInvitationNotFoundException ex) {
                return buildErrorResponse(
                                HttpStatus.NOT_FOUND,
                                "researchgroup.invitation.notfound",
                                new Object[] { ex.getInvitationId() });
        }

        @ExceptionHandler(InvalidResearchGroupInvitationRoleException.class)
        public ResponseEntity<ApiErrorResponse> handleInvalidResearchGroupInvitationRole(
                        InvalidResearchGroupInvitationRoleException ex) {
                return buildErrorResponse(
                                HttpStatus.BAD_REQUEST,
                                "researchgroup.invitation.role.invalid",
                                new Object[] { ex.getRole().name() });
        }

        @ExceptionHandler(InvalidResearchGroupMemberRoleException.class)
        public ResponseEntity<ApiErrorResponse> handleInvalidResearchGroupMemberRole(
                        InvalidResearchGroupMemberRoleException ex) {
                return buildErrorResponse(
                                HttpStatus.BAD_REQUEST,
                                "researchgroup.member.role.invalid",
                                new Object[] { ex.getRole().name() });
        }

        @ExceptionHandler(ResearchGroupMemberNotFoundException.class)
        public ResponseEntity<ApiErrorResponse> handleResearchGroupMemberNotFound(
                        ResearchGroupMemberNotFoundException ex) {
                return buildErrorResponse(
                                HttpStatus.NOT_FOUND,
                                "researchgroup.member.notfound",
                                new Object[] { ex.getGroupId(), ex.getUserId() });
        }

        @ExceptionHandler(NotificationNotFoundException.class)
        public ResponseEntity<ApiErrorResponse> handleNotificationNotFound(NotificationNotFoundException ex) {
                return buildErrorResponse(
                                HttpStatus.NOT_FOUND,
                                "notification.error.notfound",
                                new Object[] { ex.getNotificationId() });
        }

        @ExceptionHandler(ProjectNotFoundException.class)
        public ResponseEntity<ApiErrorResponse> handleProjectNotFound(ProjectNotFoundException ex) {
                return buildErrorResponse(
                                HttpStatus.NOT_FOUND,
                                "project.error.notfound",
                                new Object[] { ex.getId() });
        }

        @ExceptionHandler(InvalidProjectDatasetException.class)
        public ResponseEntity<ApiErrorResponse> handleInvalidProjectDataset(InvalidProjectDatasetException ex) {
                return buildErrorResponse(
                                HttpStatus.BAD_REQUEST,
                                "project.dataset.error.invalid",
                                new Object[] { ex.getMessage() });
        }

        @ExceptionHandler(InvalidProjectSetupException.class)
        public ResponseEntity<ApiErrorResponse> handleInvalidProjectSetup(InvalidProjectSetupException ex) {
                return buildErrorResponse(
                                HttpStatus.BAD_REQUEST,
                                "project.setup.error.invalid",
                                new Object[] { ex.getMessage() });
        }

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
                Map<String, String> details = new LinkedHashMap<>();
                for (FieldError error : ex.getBindingResult().getFieldErrors()) {
                        details.put(error.getField(), error.getDefaultMessage());
                }

                return buildErrorResponse(HttpStatus.BAD_REQUEST, "common.error.validation", null, details);
        }
}
