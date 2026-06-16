package com.transport.transport.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.transport.transport.model.Partenaire;

public interface PartenaireRepository extends MongoRepository<Partenaire, String> {
    Optional<Partenaire> findByExternalBusinessId(String externalBusinessId);
}
