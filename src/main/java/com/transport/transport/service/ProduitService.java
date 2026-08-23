package com.transport.transport.service;

import com.transport.transport.model.Produit;
import com.transport.transport.repository.ProduitRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ProduitService {

    @Autowired
    private ProduitRepository produitRepository;

    @Value("${app.public-base-url:https://www.yemchi-w-yji.tn}")
    private String publicBaseUrl;

    // Récupérer tous les produits
    public List<Produit> getAllProduits() {
        return normalizeImagesForRead(produitRepository.findAll());
    }

    // Récupérer un produit par son id
    public Optional<Produit> getProduitById(String  id) {
        return produitRepository.findById(id).map(this::normalizeImagesForRead);
    }

    // Ajouter un nouveau produit
    public Produit createProduit(Produit produit) {
        return produitRepository.save(normalizeImagesForWrite(produit));
    }

    public List<Produit> createProduits(List<Produit> produits) {
        if (produits == null || produits.isEmpty()) {
            return List.of();
        }
        return produitRepository.saveAll(
                produits.stream()
                        .map(this::normalizeImagesForWrite)
                        .collect(Collectors.toList()));
    }

    // Mettre à jour un produit existant
    public Produit updateProduit(String  id, Produit produitDetails) {
        normalizeImagesForWrite(produitDetails);
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
        return normalizeImagesForRead(produitRepository.findByCommandeId(idCommande));
    }

    public List<Produit> getProduitsByCommandeIds(List<String> commandeIds) {
        if (commandeIds == null || commandeIds.isEmpty()) {
            return List.of();
        }
        return normalizeImagesForRead(produitRepository.findByCommandeIdIn(commandeIds));
    }

    private List<Produit> normalizeImagesForRead(List<Produit> produits) {
        return produits.stream()
                .map(this::normalizeImagesForRead)
                .collect(Collectors.toList());
    }

    private Produit normalizeImagesForRead(Produit produit) {
        if (produit == null) {
            return null;
        }
        produit.setImage1(normalizeImageUrl(produit.getImage1(), false));
        produit.setImage2(normalizeImageUrl(produit.getImage2(), false));
        produit.setImage3(normalizeImageUrl(produit.getImage3(), false));
        return produit;
    }

    private Produit normalizeImagesForWrite(Produit produit) {
        if (produit == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Produit obligatoire");
        }
        produit.setImage1(normalizeImageUrl(produit.getImage1(), true));
        produit.setImage2(normalizeImageUrl(produit.getImage2(), true));
        produit.setImage3(normalizeImageUrl(produit.getImage3(), true));
        return produit;
    }

    private String normalizeImageUrl(String rawUrl, boolean rejectInvalid) {
        if (rawUrl == null || rawUrl.isBlank()) {
            return null;
        }

        String value = rawUrl.trim().replace('\\', '/');
        if (value.startsWith("//")) {
            value = "https:" + value;
        }

        try {
            URI uri = URI.create(value.replace(" ", "%20"));
            if (!uri.isAbsolute()) {
                String base = publicBaseUrl.endsWith("/") ? publicBaseUrl : publicBaseUrl + "/";
                String relative = value.startsWith("/") ? value.substring(1) : value;
                return URI.create(base).resolve(relative.replace(" ", "%20")).toString();
            }

            String scheme = uri.getScheme();
            String host = uri.getHost();
            if (!"https".equalsIgnoreCase(scheme) || host == null || host.isBlank() || isLocalHost(host)) {
                return invalidImageUrl(rejectInvalid);
            }
            return uri.toString();
        } catch (IllegalArgumentException exception) {
            return invalidImageUrl(rejectInvalid);
        }
    }

    private String invalidImageUrl(boolean rejectInvalid) {
        if (rejectInvalid) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Les images produit doivent utiliser une URL HTTPS publique");
        }
        return null;
    }

    private boolean isLocalHost(String host) {
        String normalized = host.toLowerCase();
        return "localhost".equals(normalized)
                || "127.0.0.1".equals(normalized)
                || "0.0.0.0".equals(normalized)
                || "10.0.2.2".equals(normalized)
                || normalized.endsWith(".local");
    }

}
