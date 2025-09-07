package com.transport.transport.repository;

import com.transport.transport.model.Commande;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommandeRepository extends MongoRepository<Commande, String> {
    Optional<Commande> findByClientIdAndStatut(String idClient, Commande.Statut statut);
    List<Commande> findByClientIdOrderByDateDemandeDesc(String clientId);
}
