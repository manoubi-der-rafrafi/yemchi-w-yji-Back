package com.transport.transport.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.transport.transport.model.Demande;

public interface DemandeRepository extends MongoRepository<Demande, String> {
    boolean existsByNumero(String numero);
}
