package com.transport.transport.repository;

import com.transport.transport.model.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UtilisateurRepository extends JpaRepository<Utilisateur, Integer> {
    boolean existsByEmail(String email);
    Utilisateur findByEmail(String email);
}
