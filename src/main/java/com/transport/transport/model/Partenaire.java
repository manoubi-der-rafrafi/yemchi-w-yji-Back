package com.transport.transport.model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "partenaire")
public class Partenaire {

    @Id
    private String id;

    @Indexed(unique = true, sparse = true)
    private String externalBusinessId;

    private String externalOwnerUserId;
    private String businessName;
    private Statut statut = Statut.ACTIF;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public enum Statut {
        ACTIF,
        INACTIF,
        REVOQUE
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getExternalBusinessId() {
        return externalBusinessId;
    }

    public void setExternalBusinessId(String externalBusinessId) {
        this.externalBusinessId = externalBusinessId;
    }

    public String getExternalOwnerUserId() {
        return externalOwnerUserId;
    }

    public void setExternalOwnerUserId(String externalOwnerUserId) {
        this.externalOwnerUserId = externalOwnerUserId;
    }

    public String getBusinessName() {
        return businessName;
    }

    public void setBusinessName(String businessName) {
        this.businessName = businessName;
    }

    public Statut getStatut() {
        return statut;
    }

    public void setStatut(Statut statut) {
        this.statut = statut;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
