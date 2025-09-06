package com.transport.transport.repository;

import com.transport.transport.model.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UtilisateurRepository extends JpaRepository<Utilisateur, Integer> {
    boolean existsByEmail(String email);
    Utilisateur findByEmail(String email);
    Optional<Utilisateur> findByTelephone(String telephone);
    Optional<Utilisateur> findByEmailIgnoreCase(String email);
}
