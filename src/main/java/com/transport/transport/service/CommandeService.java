package com.transport.transport.service;

import com.transport.transport.model.Commande;
import com.transport.transport.repository.CommandeRepository;
import com.transport.transport.repository.UtilisateurRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CommandeService {

    @Autowired
    private CommandeRepository commandeRepository;
    private final UtilisateurRepository utilisateurRepository;

    public CommandeService(CommandeRepository commandeRepository, UtilisateurRepository utilisateurRepository) {
        this.commandeRepository = commandeRepository;
        this.utilisateurRepository = utilisateurRepository;
    }

    // Historique simple d’un client
    public List<Commande> getHistoriqueClient(Integer clientId) {
        utilisateurRepository.findById(clientId)
                .orElseThrow(() -> new IllegalArgumentException("Client introuvable"));
        return commandeRepository.findByClientIdOrderByDateDemandeDesc(clientId);
    }
    // Récupérer toutes les commandes
    public List<Commande> getAllCommandes() {
        return commandeRepository.findAll();
    }

    // Récupérer une commande par son id
    public Optional<Commande> getCommandeById(Integer id) {
        return commandeRepository.findById(id);
    }

    // Ajouter une nouvelle commande
    public Commande createCommande(Commande commande) {
        return commandeRepository.save(commande);
    }

    // Mettre à jour une commande existante
    public Commande updateCommande(Integer id, Commande commandeDetails) {
        Optional<Commande> optionalCommande = commandeRepository.findById(id);
        if (optionalCommande.isPresent()) {
            Commande commande = optionalCommande.get();
            // Met à jour les champs ici selon besoin
            commande.setLocalisationDepart(commandeDetails.getLocalisationDepart());
            commande.setDestination(commandeDetails.getDestination());
            commande.setDateDebut(commandeDetails.getDateDebut());
            commande.setDateFin(commandeDetails.getDateFin());
            commande.setDateDemande(commandeDetails.getDateDemande());
            commande.setStatut(commandeDetails.getStatut());
            commande.setPrix(commandeDetails.getPrix());
            commande.setModePaiement(commandeDetails.getModePaiement());
            commande.setInstructions(commandeDetails.getInstructions());
            commande.setClient(commandeDetails.getClient());
            commande.setTransporteur(commandeDetails.getTransporteur());
            return commandeRepository.save(commande);
        } else {
            return null; // ou tu peux gérer une exception personnalisée
        }
    }

    // Supprimer une commande par son id
    public void deleteCommande(Integer id) {
        commandeRepository.deleteById(id);
    }
    public Optional<Commande> getCommandeEnCoursByClientId(Integer idClient) {
        return commandeRepository.findByClientIdAndStatut(idClient, Commande.Statut.en_cours);
    }
}
