package com.transport.transport.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.transport.transport.model.RefreshTokenFamily;

public interface RefreshTokenFamilyRepository extends MongoRepository<RefreshTokenFamily, String> {
}
