package com.transport.transport.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.transport.transport.model.Notification;
import com.transport.transport.model.Utilisateur;
import com.transport.transport.service.AuthorizationService;
import com.transport.transport.service.NotificationService;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final AuthorizationService authorizationService;

    public NotificationController(NotificationService notificationService, AuthorizationService authorizationService) {
        this.notificationService = notificationService;
        this.authorizationService = authorizationService;
    }

    @GetMapping
    public List<Notification> mesNotifications(Authentication authentication) {
        Utilisateur current = authorizationService.currentUser(authentication);
        return notificationService.lister(current.getId());
    }

    @GetMapping("/non-lues/count")
    public Map<String, Long> compterNonLues(Authentication authentication) {
        Utilisateur current = authorizationService.currentUser(authentication);
        return Map.of("count", notificationService.compterNonLues(current.getId()));
    }

    @PutMapping("/{id}/lu")
    public ResponseEntity<Notification> marquerLue(@PathVariable String id, Authentication authentication) {
        Utilisateur current = authorizationService.currentUser(authentication);
        try {
            return ResponseEntity.ok(notificationService.marquerLue(id, current.getId()));
        } catch (SecurityException e) {
            return ResponseEntity.status(403).build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
