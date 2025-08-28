package com.transport.transport.service;

import com.transport.transport.model.Utilisateur;
import com.transport.transport.repository.UtilisateurRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UtilisateurService {

    private final UtilisateurRepository repo;

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    public Utilisateur saveUtilisateur(Utilisateur utilisateur) {
        utilisateur.setStatut(Utilisateur.Statut.valueOf("actif")); // ou ce que tu veux par défaut
        utilisateur.setRole(Utilisateur.Role.valueOf("client"));  // ou "user", "utilisateur", à adapter à ton besoin

        return utilisateurRepository.save(utilisateur);
    }

    public UtilisateurService(UtilisateurRepository repo) {
        this.repo = repo;
    }

    public List<Utilisateur> findAll() {
        return repo.findAll();
    }

    public Utilisateur save(Utilisateur utilisateur) {
        return repo.save(utilisateur);
    }

    public Utilisateur findById(Integer id) {
        return repo.findById(id).orElse(null);
    }

    public void deleteById(Integer id) {
        repo.deleteById(id);
    }
}
