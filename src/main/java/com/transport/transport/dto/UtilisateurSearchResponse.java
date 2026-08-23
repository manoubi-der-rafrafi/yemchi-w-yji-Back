package com.transport.transport.dto;

import com.transport.transport.model.Utilisateur;

/**
 * Profil public minimal retourne par la recherche d'utilisateurs.
 *
 * <p>Ne jamais remplacer ce DTO par l'entite {@link Utilisateur}: celle-ci
 * contient notamment le mot de passe, les documents d'identite et la position.
 */
public record UtilisateurSearchResponse(
    String id,
    String nom,
    String prenom,
    String email,
    String telephone,
    String image,
    Utilisateur.Role role) {

  public static UtilisateurSearchResponse from(Utilisateur utilisateur) {
    return new UtilisateurSearchResponse(
        utilisateur.getId(),
        utilisateur.getNom(),
        utilisateur.getPrenom(),
        utilisateur.getEmail(),
        utilisateur.getTelephone(),
        utilisateur.getImage(),
        utilisateur.getRole());
  }
}
