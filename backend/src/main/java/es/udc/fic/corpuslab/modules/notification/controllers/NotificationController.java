package es.udc.fic.corpuslab.modules.notification.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import es.udc.fic.corpuslab.modules.notification.dtos.NotificationDto;
import es.udc.fic.corpuslab.modules.notification.dtos.NotificationListResponseDto;
import es.udc.fic.corpuslab.modules.notification.services.NotificationService;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public NotificationListResponseDto myNotifications(
            Authentication authentication,
            @RequestParam(defaultValue = "10") int limit) {
        return notificationService.findMyNotifications(authentication.getName(), limit);
    }

    @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(Authentication authentication) {
        return notificationService.openNotificationStream(authentication.getName());
    }

    @PostMapping("/{notificationId}/read")
    public NotificationDto markAsRead(
            Authentication authentication,
            @PathVariable Long notificationId) {
        return notificationService.markNotificationAsRead(authentication.getName(), notificationId);
    }

    @PostMapping("/read-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAllAsRead(Authentication authentication) {
        notificationService.markAllNotificationsAsRead(authentication.getName());
    }
}
