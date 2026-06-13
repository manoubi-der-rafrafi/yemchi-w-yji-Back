package com.transport.transport.service;

import com.transport.transport.model.Produit;
import com.transport.transport.repository.ProduitRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProduitService {

    @Autowired
    private ProduitRepository produitRepository;

    // Récupérer tous les produits
    public List<Produit> getAllProduits() {
        return produitRepository.findAll();
    }

    // Récupérer un produit par son id
    public Optional<Produit> getProduitById(String  id) {
        return produitRepository.findById(id);
    }

    // Ajouter un nouveau produit
    public Produit createProduit(Produit produit) {
        return produitRepository.save(produit);
    }

    // Mettre à jour un produit existant
    public Produit updateProduit(String  id, Produit produitDetails) {
        Optional<Produit> optionalProduit = produitRepository.findById(id);
        if (optionalProduit.isPresent()) {
            Produit produit = optionalProduit.get();
            produit.setNom(produitDetails.getNom());
            produit.setType(produitDetails.getType());
            produit.setLargeur(produitDetails.getLargeur());
            produit.setProfondeur(produitDetails.getProfondeur());
            produit.setHauteur(produitDetails.getHauteur());
            produit.setPoids(produitDetails.getPoids());
            produit.setQuantite(produitDetails.getQuantite());
            produit.setAffecter(produitDetails.getAffecter());
            produit.setQuantiteAffecter(produitDetails.getQuantiteAffecter());
            produit.setFacade(produitDetails.getFacade());
            produit.setDescription(produitDetails.getDescription());
            produit.setImage1(produitDetails.getImage1());
            produit.setImage2(produitDetails.getImage2());
            produit.setImage3(produitDetails.getImage3());
            produit.setCommandeId(produitDetails.getCommandeId());
            produit.setPrix(produitDetails.getPrix());
            return produitRepository.save(produit);
        } else {
            return null; // ou tu peux lancer une exception personnalisée
        }
    }

    // Supprimer un produit par son id
    public void deleteProduit(String  id) {
        produitRepository.deleteById(id);
    }
    public List<Produit> getProduitsByCommandeId(String  idCommande) {
        return produitRepository.findByCommandeId(idCommande);
    }

    public List<Produit> getProduitsByCommandeIds(List<String> commandeIds) {
        if (commandeIds == null || commandeIds.isEmpty()) {
            return List.of();
        }
        return produitRepository.findByCommandeIdIn(commandeIds);
    }

}
