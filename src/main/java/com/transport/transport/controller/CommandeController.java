package com.transport.transport.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.transport.transport.model.Commande;
import com.transport.transport.model.TypeVehicule;
import com.transport.transport.service.CommandeService;

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
    public ResponseEntity<Commande> getCommandeById(@PathVariable String  id) {
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
    public ResponseEntity<Commande> updateCommande(@PathVariable String  id, @RequestBody Commande commandeDetails) {
        Commande updatedCommande = commandeService.updateCommande(id, commandeDetails);
        if (updatedCommande != null) {
            return ResponseEntity.ok(updatedCommande);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Commande> updateCommandePatch(@PathVariable String  id, @RequestBody Commande commandeDetails) {
        Commande updatedCommande = commandeService.updateCommandePatch(id, commandeDetails);
        if (updatedCommande != null) {
            return ResponseEntity.ok(updatedCommande);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    // DELETE : Supprimer une commande
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCommande(@PathVariable String  id) {
        commandeService.deleteCommande(id);
        return ResponseEntity.noContent().build();
    }
    @GetMapping("/client/{idClient}/en_cours")
    public ResponseEntity<Commande> getCommandeEnCoursByClient(@PathVariable String  idClient) {
        return commandeService.getCommandeEnCoursByClientId(idClient)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    // Historique simple d’un client
    @GetMapping("/client/{clientId}")
    public ResponseEntity<?> getHistoriqueClient(@PathVariable String  clientId) {
        try {
            List<Commande> historique = commandeService.getHistoriqueClient(clientId);
            return ResponseEntity.ok(historique);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    @PutMapping("/{id}/confirmer")
    public ResponseEntity<Commande> confirmerCommande(@PathVariable String  id) {
        try {
            Commande commande = commandeService.confirmerCommande(id);
            return ResponseEntity.ok(commande);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
    @GetMapping("/ami/{idAmie}")
    public List<Commande> getByIdAmie(@PathVariable String idAmie) {
        return commandeService.getByIdAmie(idAmie);
    }
    @GetMapping("/ami/{idAmie}/count")
    public ResponseEntity<Integer> countCommandesByIdAmie(@PathVariable String idAmie) {
        int count = commandeService.countCommandesByIdAmie(idAmie);
        return ResponseEntity.ok(count);
    }
    @GetMapping("/ami/{idAmie}/count/envoyee")
    public ResponseEntity<Integer> countByIdAmieAndStatutEnvoyee(@PathVariable String idAmie) {
        int count = commandeService.countCommandesByIdAmieAndStatutEnvoyee(idAmie);
        return ResponseEntity.ok(count);
    }
    @GetMapping("/zone/{zone}")
    public List<Commande> getCommandesByZonePrincipale(@PathVariable Commande.Zone zone) {
        return commandeService.getCommandesByZonePrincipale(zone);
    }

    @GetMapping("/zone/{zone}/confirmees")
    public List<Commande> getCommandesConfirmeesByZone(@PathVariable Commande.Zone zone) {
        return commandeService.getCommandesByZonePrincipaleConfirmees(zone);
    }
    @GetMapping("/zone/{zone}/vehicule/{vehicule}")
    public ResponseEntity<List<Commande>> getCommandesByZoneAndVehicule(
            @PathVariable Commande.Zone zone,
            @PathVariable TypeVehicule vehicule) {
        try {
            return ResponseEntity.ok(commandeService.getCommandesByZonePrincipaleAndVehicule(zone, vehicule));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    @PutMapping("/{idCommande}/assigner/{idTransporteur}")
public ResponseEntity<Commande> assignerTransporteur(
        @PathVariable String idCommande,
        @PathVariable String idTransporteur) {
    try {
        Commande commande = commandeService.assignerTransporteur(idCommande, idTransporteur);
        return ResponseEntity.ok(commande);
    } catch (IllegalStateException e) {
        return ResponseEntity.badRequest().build();
    } catch (IllegalArgumentException e) {
        return ResponseEntity.notFound().build();
    }
}
@GetMapping("/transporteur/{idTransporteur}")
public List<Commande> getByTransporteur(@PathVariable String idTransporteur) {
    return commandeService.getCommandesByTransporteur(idTransporteur);
}
@GetMapping("/transporteur/{idTransporteur}/total-livree")
public ResponseEntity<BigDecimal> getSommePrixLivreeByTransporteur(@PathVariable String idTransporteur) {
    BigDecimal total = commandeService.getSommePrixCommandesLivreesByTransporteur(idTransporteur);
    return ResponseEntity.ok(total);
}
@GetMapping("/transporteur/{idTransporteur}/total-livree-en-ligne")
public ResponseEntity<BigDecimal> getSommePrixLivreeEnLigneByTransporteur(@PathVariable String idTransporteur) {
    BigDecimal total = commandeService.getSommePrixCommandesLivreesEnLigneByTransporteur(idTransporteur);
    return ResponseEntity.ok(total);
}
@GetMapping("/transporteur/{idTransporteur}/total-livree-hors-ligne")
public ResponseEntity<BigDecimal> getSommePrixLivreeHorsLigneByTransporteur(@PathVariable String idTransporteur) {
    BigDecimal total = commandeService.getSommePrixCommandesLivreesHorsLigneByTransporteur(idTransporteur);
    return ResponseEntity.ok(total);
}
@PostMapping("/sous-zones")
public ResponseEntity<List<Commande>> getCommandesBySousZones(@RequestBody SousZoneFilterRequest request) {
    if (request == null) {
        return ResponseEntity.badRequest().build();
    }
    try {
        List<Commande> commandes = commandeService.getCommandesBySousZones(
                request.getSousZonesDepart(),
                request.getSousZonesArrivee());
        return ResponseEntity.ok(commandes);
    } catch (IllegalArgumentException e) {
        return ResponseEntity.badRequest().build();
    }
}
@PostMapping("/sous-zones/vehicule")
public ResponseEntity<List<Commande>> getCommandesBySousZonesAndVehicule(
        @RequestBody SousZoneVehiculeFilterRequest request) {
    if (request == null) {
        return ResponseEntity.badRequest().build();
    }
    try {
        List<Commande> commandes = commandeService.getCommandesBySousZonesAndVehicule(
                request.getSousZonesDepart(),
                request.getSousZonesArrivee(),
                request.getVehicule());
        return ResponseEntity.ok(commandes);
    } catch (IllegalArgumentException e) {
        return ResponseEntity.badRequest().build();
    }
}

@PutMapping("/{id}/scan-depart")
public ResponseEntity<Commande> marquerDepartScanne(@PathVariable String id) {
    try {
        Commande commande = commandeService.marquerDepartScanne(id);
        return ResponseEntity.ok(commande);
    } catch (IllegalArgumentException e) {
        return ResponseEntity.notFound().build();
    }
}

@PutMapping("/{id}/scan-reception")
public ResponseEntity<Commande> marquerReceptionScanne(@PathVariable String id) {
    try {
        Commande commande = commandeService.marquerReceptionScanne(id);
        return ResponseEntity.ok(commande);
    } catch (IllegalArgumentException e) {
        return ResponseEntity.notFound().build();
    }
}

public static class SousZoneFilterRequest {
    private List<Commande.SousZone> sousZonesDepart;
    private List<Commande.SousZone> sousZonesArrivee;

    public List<Commande.SousZone> getSousZonesDepart() {
        return sousZonesDepart;
    }

    public void setSousZonesDepart(List<Commande.SousZone> sousZonesDepart) {
        this.sousZonesDepart = sousZonesDepart;
    }

    public List<Commande.SousZone> getSousZonesArrivee() {
        return sousZonesArrivee;
    }

    public void setSousZonesArrivee(List<Commande.SousZone> sousZonesArrivee) {
        this.sousZonesArrivee = sousZonesArrivee;
    }
}
public static class SousZoneVehiculeFilterRequest {
    private List<Commande.SousZone> sousZonesDepart;
    private List<Commande.SousZone> sousZonesArrivee;
    private TypeVehicule vehicule;

    public List<Commande.SousZone> getSousZonesDepart() {
        return sousZonesDepart;
    }

    public void setSousZonesDepart(List<Commande.SousZone> sousZonesDepart) {
        this.sousZonesDepart = sousZonesDepart;
    }

    public List<Commande.SousZone> getSousZonesArrivee() {
        return sousZonesArrivee;
    }

    public void setSousZonesArrivee(List<Commande.SousZone> sousZonesArrivee) {
        this.sousZonesArrivee = sousZonesArrivee;
    }

    public TypeVehicule getVehicule() {
        return vehicule;
    }

    public void setVehicule(TypeVehicule vehicule) {
        this.vehicule = vehicule;
    }
}

}
