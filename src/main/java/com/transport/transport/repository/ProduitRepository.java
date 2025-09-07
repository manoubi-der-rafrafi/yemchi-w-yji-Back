package com.transport.transport.repository;

import com.transport.transport.model.Produit;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProduitRepository extends MongoRepository<Produit, String> {
    List<Produit> findByCommandeId(String  idCommande);
}
