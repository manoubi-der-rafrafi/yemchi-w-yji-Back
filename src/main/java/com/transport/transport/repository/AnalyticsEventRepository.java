package com.transport.transport.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.transport.transport.model.AnalyticsEvent;

public interface AnalyticsEventRepository extends MongoRepository<AnalyticsEvent, String> {
  long countByCreatedAtBetween(Instant from, Instant to);
  List<AnalyticsEvent> findByCreatedAtBetweenOrderByCreatedAtDesc(Instant from, Instant to);
}
