package com.transport.transport.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import com.transport.transport.model.Facture;

@Repository
public interface FactureRepository extends MongoRepository<Facture, String> {
    java.util.List<Facture> findByIdLivreur(String idLivreur);
    java.util.List<Facture> findByIdLivreurAndType(String idLivreur, Facture.FactureType type);
    @Query("{ 'id_livreur': ?0, 'confirmer': { $ne: 'NON_TRAITER' } }")
    java.util.List<Facture> findByIdLivreurAndConfirmerNotNonTraiter(String idLivreur);
}
