package com.transport.transport.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.transport.transport.model.ApplicationError;

public interface ApplicationErrorRepository extends MongoRepository<ApplicationError, String> {
}
