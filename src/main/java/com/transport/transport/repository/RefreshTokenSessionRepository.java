package com.transport.transport.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.transport.transport.model.RefreshTokenSession;

public interface RefreshTokenSessionRepository extends MongoRepository<RefreshTokenSession, String> {
  Optional<RefreshTokenSession> findByTokenHash(String tokenHash);
}
