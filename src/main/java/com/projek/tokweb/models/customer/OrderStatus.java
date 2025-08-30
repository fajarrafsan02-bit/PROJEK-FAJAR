package com.projek.tokweb.models.customer;

public enum OrderStatus {
    PENDING_PAYMENT("Menunggu Pembayaran"),
    PENDING_CONFIRMATION("Menunggu Konfirmasi"),
    PAID("Sudah Dibayar"),
    PROCESSING("Sedang Diproses"),
    SHIPPED("Dikirim"),
    DELIVERED("Terkirim"),
    CANCELLED("Dibatalkan"),
    REFUNDED("Dikembalikan");
    
    private final String displayName;
    
    OrderStatus(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public boolean isActive() {
        return this != CANCELLED && this != REFUNDED;
    }
    
    public boolean canTransitionTo(OrderStatus newStatus) {
        switch (this) {
            case PENDING_PAYMENT:
                return newStatus == PENDING_CONFIRMATION || newStatus == PAID || newStatus == CANCELLED;
            case PENDING_CONFIRMATION:
                return newStatus == PAID || newStatus == PROCESSING || newStatus == CANCELLED;
            case PAID:
                return newStatus == PROCESSING || newStatus == CANCELLED;
            case PROCESSING:
                return newStatus == SHIPPED || newStatus == CANCELLED;
            case SHIPPED:
                return newStatus == DELIVERED || newStatus == CANCELLED;
            case DELIVERED:
                return newStatus == REFUNDED;
            case CANCELLED:
            case REFUNDED:
                return false;
            default:
                return false;
        }
    }
}