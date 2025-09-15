package com.transport.transport.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import com.transport.transport.model.Utilisateur;
public interface UtilisateurRepository extends MongoRepository<Utilisateur, String> {
    boolean existsByEmail(String email);
    Utilisateur findByEmail(String email);
    Optional<Utilisateur> findByTelephone(String telephone);
    Optional<Utilisateur> findByEmailIgnoreCase(String email);
    @Query("{ '_id': { $in: ?0 }, 'telephone': { $regex: ?1, $options: 'i' } }")
    List<Utilisateur> findByIdInAndTelephoneRegex(List<String> ids, String telephoneRegex);
    // 🔎 recherche PARTIELLE (contient, insensible à la casse) parmi un set d'IDs
    List<Utilisateur> findByIdInAndEmailContainingIgnoreCase(List<String> ids, String emailPart);

    // 🔎 recherche EXACTE (insensible à la casse) parmi un set d'IDs
    Optional<Utilisateur> findFirstByIdInAndEmailIgnoreCase(List<String> ids, String email);
}
