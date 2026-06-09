package es.udc.fic.corpuslab.modules.notification.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import es.udc.fic.corpuslab.modules.notification.services.NotificationService;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private Authentication authentication;

    private NotificationController controller;

    @BeforeEach
    void setUp() {
        controller = new NotificationController(notificationService);
        when(authentication.getName()).thenReturn("recipient@example.com");
    }

    @Test
    void streamShouldDelegateToServiceAndReturnEmitter() {
        SseEmitter expected = new SseEmitter();
        when(notificationService.openNotificationStream("recipient@example.com")).thenReturn(expected);

        SseEmitter result = controller.stream(authentication);

        assertThat(result).isSameAs(expected);
        verify(notificationService).openNotificationStream("recipient@example.com");
    }
}
