package com.transport.transport.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.transport.transport.model.EtatBlocageLivreur;

public interface EtatBlocageLivreurRepository extends MongoRepository<EtatBlocageLivreur, String> {
    Optional<EtatBlocageLivreur> findByLivreurId(String livreurId);
}
