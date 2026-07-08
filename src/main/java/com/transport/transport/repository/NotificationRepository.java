package com.transport.transport.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.transport.transport.model.Notification;

@Repository
public interface NotificationRepository extends MongoRepository<Notification, String> {
    List<Notification> findByDestinataireIdOrderByCreeLeDesc(String destinataireId);
    long countByDestinataireIdAndLuFalse(String destinataireId);
}
