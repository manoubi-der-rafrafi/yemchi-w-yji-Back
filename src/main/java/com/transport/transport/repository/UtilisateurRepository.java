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
    @Query("{ '_id': { $in: ?0 }, $or: [ { 'nom': { $regex: ?1 } }, { 'prenom': { $regex: ?1 } } ] }")
List<Utilisateur> findByIdInAndNomOrPrenomRegex(List<String> ids, String regex);
@Query("{ '_id': { $in: ?0 }, " +
       "  'nom':    { $regex: ?1, $options: 'i' }, " +
       "  'prenom': { $regex: ?2, $options: 'i' } " +
       "}")
List<Utilisateur> searchByIdsAndNomAndPrenomRegex(List<String> ids, String nomRegex, String prenomRegex);
  // ----- Dérivation de requête (fonctionne en Mongo & JPA) -----
  List<Utilisateur> findByIdInAndNomContainingIgnoreCase(List<String> ids, String nom);
  List<Utilisateur> findByIdInAndPrenomContainingIgnoreCase(List<String> ids, String prenom);

  // nom OU prénom (une seule chaîne "q" tapée par l’utilisateur)
  List<Utilisateur> findByIdInAndNomContainingIgnoreCaseOrIdInAndPrenomContainingIgnoreCase(
      List<String> ids1, String nom,
      List<String> ids2, String prenom
  );

  // nom ET prénom (deux champs séparés)
  List<Utilisateur> findByIdInAndNomContainingIgnoreCaseAndPrenomContainingIgnoreCase(
      List<String> ids, String nom, String prenom
  );

  // ----- (Optionnel Mongo uniquement) Version regex plus souple -----
@Query("{ '_id': { $in: ?0 }, $or: [ " +
       "  { 'nom':    { $regex: ?1, $options: 'i' } }, " +
       "  { 'prenom': { $regex: ?1, $options: 'i' } } " +
       "] }")
List<Utilisateur> searchByIdsAndNomOrPrenomRegex(List<String> ids, String qRegex);

}
