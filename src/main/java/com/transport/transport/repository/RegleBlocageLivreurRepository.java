package com.transport.transport.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.transport.transport.model.RegleBlocageLivreur;

public interface RegleBlocageLivreurRepository extends MongoRepository<RegleBlocageLivreur, String> {
    Optional<RegleBlocageLivreur> findFirstByDateFinIsNullOrderByDateDebutDesc();
}
