package com.transport.transport.controller;

import com.transport.transport.model.Commande;
import com.transport.transport.service.CommandeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/commandes")
public class CommandeController {

    @Autowired
    private CommandeService commandeService;

    // GET : Liste de toutes les commandes
    @GetMapping
    public List<Commande> getAllCommandes() {
        return commandeService.getAllCommandes();
    }

    // GET : Récupérer une commande par id
    @GetMapping("/{id}")
    public ResponseEntity<Commande> getCommandeById(@PathVariable Integer id) {
        Optional<Commande> commande = commandeService.getCommandeById(id);
        return commande.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // POST : Créer une nouvelle commande
    @PostMapping
    public Commande createCommande(@RequestBody Commande commande) {
        return commandeService.createCommande(commande);
    }

    // PUT : Modifier une commande existante
    @PutMapping("/{id}")
    public ResponseEntity<Commande> updateCommande(@PathVariable Integer id, @RequestBody Commande commandeDetails) {
        Commande updatedCommande = commandeService.updateCommande(id, commandeDetails);
        if (updatedCommande != null) {
            return ResponseEntity.ok(updatedCommande);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    // DELETE : Supprimer une commande
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCommande(@PathVariable Integer id) {
        commandeService.deleteCommande(id);
        return ResponseEntity.noContent().build();
    }
    @GetMapping("/client/{idClient}/en_cours")
    public ResponseEntity<Commande> getCommandeEnCoursByClient(@PathVariable Integer idClient) {
        return commandeService.getCommandeEnCoursByClientId(idClient)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    // Historique simple d’un client
    @GetMapping("/client/{clientId}")
    public ResponseEntity<?> getHistoriqueClient(@PathVariable Integer clientId) {
        try {
            List<Commande> historique = commandeService.getHistoriqueClient(clientId);
            return ResponseEntity.ok(historique);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
