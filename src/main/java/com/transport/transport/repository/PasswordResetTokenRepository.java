package com.transport.transport.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.transport.transport.model.PasswordResetToken;

public interface PasswordResetTokenRepository
    extends MongoRepository<PasswordResetToken, String> {
}
