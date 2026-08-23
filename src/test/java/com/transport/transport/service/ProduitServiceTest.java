package com.transport.transport.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import com.transport.transport.model.Produit;
import com.transport.transport.repository.ProduitRepository;

class ProduitServiceTest {

    @Test
    void resolvesLegacyRelativeImageUrlsWhenReadingProducts() {
        ProduitRepository repository = mock(ProduitRepository.class);
        Produit produit = new Produit();
        produit.setImage1("/produits/mon image.jpg");
        when(repository.findByCommandeId("commande-1")).thenReturn(List.of(produit));

        ProduitService service = serviceWith(repository);

        List<Produit> result = service.getProduitsByCommandeId("commande-1");

        assertEquals(
                "https://www.yemchi-w-yji.tn/produits/mon%20image.jpg",
                result.get(0).getImage1());
    }

    @Test
    void preservesPublicHttpsImageUrlsWhenCreatingProducts() {
        ProduitRepository repository = mock(ProduitRepository.class);
        when(repository.save(any(Produit.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Produit produit = new Produit();
        produit.setImage1("https://res.cloudinary.com/demo/image/upload/sample.jpg");

        Produit result = serviceWith(repository).createProduit(produit);

        assertEquals(
                "https://res.cloudinary.com/demo/image/upload/sample.jpg",
                result.getImage1());
    }

    @Test
    void rejectsLocalImageUrlsWhenCreatingProducts() {
        ProduitRepository repository = mock(ProduitRepository.class);
        Produit produit = new Produit();
        produit.setImage1("http://localhost:4200/produits/image.jpg");

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> serviceWith(repository).createProduit(produit));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    private ProduitService serviceWith(ProduitRepository repository) {
        ProduitService service = new ProduitService();
        ReflectionTestUtils.setField(service, "produitRepository", repository);
        ReflectionTestUtils.setField(service, "publicBaseUrl", "https://www.yemchi-w-yji.tn");
        return service;
    }
}
