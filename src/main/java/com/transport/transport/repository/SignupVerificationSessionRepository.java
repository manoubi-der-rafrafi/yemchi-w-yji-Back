package com.transport.transport.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.transport.transport.model.SignupVerificationSession;

public interface SignupVerificationSessionRepository
    extends MongoRepository<SignupVerificationSession, String> {
  Optional<SignupVerificationSession> findByClientTokenHash(String clientTokenHash);
}
