package com.transport.transport.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.transport.transport.model.Notification;
import com.transport.transport.repository.NotificationRepository;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public Notification creer(
            String destinataireId,
            Notification.Type type,
            String titre,
            String message,
            String referenceId,
            String acteurId,
            Map<String, String> data) {
        if (destinataireId == null || destinataireId.isBlank()) {
            throw new IllegalArgumentException("destinataireId obligatoire");
        }
        Notification notification = new Notification();
        notification.setDestinataireId(destinataireId);
        notification.setType(type);
        notification.setTitre(titre);
        notification.setMessage(message);
        notification.setReferenceId(referenceId);
        notification.setActeurId(acteurId);
        notification.setData(data);
        notification.setCreeLe(LocalDateTime.now());
        return notificationRepository.save(notification);
    }

    public List<Notification> lister(String destinataireId) {
        return notificationRepository.findByDestinataireIdOrderByCreeLeDesc(destinataireId);
    }

    public long compterNonLues(String destinataireId) {
        return notificationRepository.countByDestinataireIdAndLuFalse(destinataireId);
    }

    public Notification marquerLue(String id, String destinataireId) {
        return notificationRepository.findById(id).map(notification -> {
            if (!destinataireId.equals(notification.getDestinataireId())) {
                throw new SecurityException("Acces refuse");
            }
            notification.setLu(true);
            notification.setLuLe(LocalDateTime.now());
            return notificationRepository.save(notification);
        }).orElseThrow(() -> new IllegalArgumentException("Notification introuvable"));
    }
}
