package com.transport.transport.model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "ecommerce_status_sync_event")
public class EcommerceStatusSyncEvent {

    @Id
    private String id;
    private String commandeId;
    private String externalOrderId;
    private String transportOrderId;
    private String transportStatus;
    private String ecommerceStatus;
    private LocalDateTime occurredAt;
    private LocalDateTime createdAt;
    private LocalDateTime nextAttemptAt;
    private LocalDateTime completedAt;
    private LocalDateTime failedPermanentlyAt;
    private int attempts;
    private String lastError;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getCommandeId() { return commandeId; }
    public void setCommandeId(String commandeId) { this.commandeId = commandeId; }
    public String getExternalOrderId() { return externalOrderId; }
    public void setExternalOrderId(String externalOrderId) { this.externalOrderId = externalOrderId; }
    public String getTransportOrderId() { return transportOrderId; }
    public void setTransportOrderId(String transportOrderId) { this.transportOrderId = transportOrderId; }
    public String getTransportStatus() { return transportStatus; }
    public void setTransportStatus(String transportStatus) { this.transportStatus = transportStatus; }
    public String getEcommerceStatus() { return ecommerceStatus; }
    public void setEcommerceStatus(String ecommerceStatus) { this.ecommerceStatus = ecommerceStatus; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
    public void setOccurredAt(LocalDateTime occurredAt) { this.occurredAt = occurredAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getNextAttemptAt() { return nextAttemptAt; }
    public void setNextAttemptAt(LocalDateTime nextAttemptAt) { this.nextAttemptAt = nextAttemptAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public LocalDateTime getFailedPermanentlyAt() { return failedPermanentlyAt; }
    public void setFailedPermanentlyAt(LocalDateTime value) { this.failedPermanentlyAt = value; }
    public int getAttempts() { return attempts; }
    public void setAttempts(int attempts) { this.attempts = attempts; }
    public String getLastError() { return lastError; }
    public void setLastError(String lastError) { this.lastError = lastError; }
}
