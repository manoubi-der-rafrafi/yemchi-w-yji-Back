package com.transport.transport.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.transport.transport.model.Commande;
import com.transport.transport.repository.CommandeRepository;
import com.transport.transport.repository.UtilisateurRepository;

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
    public List<Commande> getHistoriqueClient(String  clientId) {
        utilisateurRepository.findById(clientId)
                .orElseThrow(() -> new IllegalArgumentException("Client introuvable"));
        return commandeRepository.findByClientIdOrderByDateDemandeDesc(clientId);
    }
    // Récupérer toutes les commandes
    public List<Commande> getAllCommandes() {
        return commandeRepository.findAll();
    }

    // Récupérer une commande par son id
    public Optional<Commande> getCommandeById(String  id) {
        return commandeRepository.findById(id);
    }

    // Ajouter une nouvelle commande
    public Commande createCommande(Commande commande) {
        return commandeRepository.save(commande);
    }

    // Mettre à jour une commande existante
    public Commande updateCommande(String id, Commande commandeDetails) {
    Optional<Commande> optionalCommande = commandeRepository.findById(id);
    if (optionalCommande.isPresent()) {
        Commande commande = optionalCommande.get();
        // Met à jour les champs
        commande.setLocalisationDepart(commandeDetails.getLocalisationDepart());
        commande.setDestination(commandeDetails.getDestination());
        commande.setDateDebut(commandeDetails.getDateDebut());
        commande.setDateFin(commandeDetails.getDateFin());
        commande.setDateDemande(commandeDetails.getDateDemande());
        commande.setStatut(commandeDetails.getStatut());
        commande.setPrix(commandeDetails.getPrix());
        commande.setModePaiement(commandeDetails.getModePaiement());
        commande.setInstructions(commandeDetails.getInstructions());
        commande.setClientId(commandeDetails.getClientId());
        commande.setTransporteurId(commandeDetails.getTransporteurId());

        // ✅ Ajout de la mise à jour du téléphone de départ
        commande.setTelDepart(commandeDetails.getTelDepart());

        return commandeRepository.save(commande);
    } else {
        return null; // ou lancer une exception personnalisée
    }
}


    // Supprimer une commande par son id
    public void deleteCommande(String  id) {
        commandeRepository.deleteById(id);
    }
    public Optional<Commande> getCommandeEnCoursByClientId(String  idClient) {
        return commandeRepository.findByClientIdAndStatut(idClient, Commande.Statut.en_cours);
    }
    public Commande confirmerCommande(String  id) {
        return commandeRepository.findById(id).map(commande -> {
            commande.setStatut(Commande.Statut.confirmer);
            commande.setDateDemande(LocalDateTime.now()); // ➝ date actuelle
            return commandeRepository.save(commande);
        }).orElseThrow(() -> new IllegalArgumentException("Commande introuvable"));
    }

}
