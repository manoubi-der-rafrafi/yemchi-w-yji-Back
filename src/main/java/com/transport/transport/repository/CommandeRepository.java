package com.transport.transport.repository;

import com.transport.transport.model.Commande;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommandeRepository extends JpaRepository<Commande, Integer> {
    Optional<Commande> findByClientIdAndStatut(Integer idClient, Commande.Statut statut);
    List<Commande> findByClientIdOrderByDateDemandeDesc(Integer clientId);
}
