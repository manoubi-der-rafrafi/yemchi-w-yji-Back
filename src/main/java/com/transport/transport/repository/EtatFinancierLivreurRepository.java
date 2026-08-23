package com.transport.transport.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.transport.transport.model.EtatFinancierLivreur;

public interface EtatFinancierLivreurRepository extends MongoRepository<EtatFinancierLivreur, String> {
    Optional<EtatFinancierLivreur> findByLivreurId(String livreurId);
}
