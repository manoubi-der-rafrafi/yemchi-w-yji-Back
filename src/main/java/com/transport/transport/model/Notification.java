package com.transport.transport.model;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "notification")
public class Notification {

    @Id
    private String id;

    private String destinataireId;
    private String acteurId;
    private Type type;
    private String titre;
    private String message;
    private String referenceId;
    private Map<String, String> data;
    private boolean lu = false;
    private LocalDateTime creeLe = LocalDateTime.now();
    private LocalDateTime luLe;

    public enum Type {
        COMMANDE_ENVOYEE,
        INVITATION_AMI,
        ETAT_COMMANDE,
        NOUVELLE_COMMANDE
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getDestinataireId() { return destinataireId; }
    public void setDestinataireId(String destinataireId) { this.destinataireId = destinataireId; }

    public String getActeurId() { return acteurId; }
    public void setActeurId(String acteurId) { this.acteurId = acteurId; }

    public Type getType() { return type; }
    public void setType(Type type) { this.type = type; }

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getReferenceId() { return referenceId; }
    public void setReferenceId(String referenceId) { this.referenceId = referenceId; }

    public Map<String, String> getData() { return data; }
    public void setData(Map<String, String> data) { this.data = data; }

    public boolean isLu() { return lu; }
    public void setLu(boolean lu) { this.lu = lu; }

    public LocalDateTime getCreeLe() { return creeLe; }
    public void setCreeLe(LocalDateTime creeLe) { this.creeLe = creeLe; }

    public LocalDateTime getLuLe() { return luLe; }
    public void setLuLe(LocalDateTime luLe) { this.luLe = luLe; }
}
