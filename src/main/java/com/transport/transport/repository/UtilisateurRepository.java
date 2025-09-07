package com.transport.transport.repository;

import com.transport.transport.model.Utilisateur;

import java.util.Optional;
import com.transport.transport.model.Utilisateur;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;
public interface UtilisateurRepository extends MongoRepository<Utilisateur, String> {
    boolean existsByEmail(String email);
    Utilisateur findByEmail(String email);
    Optional<Utilisateur> findByTelephone(String telephone);
    Optional<Utilisateur> findByEmailIgnoreCase(String email);
}
