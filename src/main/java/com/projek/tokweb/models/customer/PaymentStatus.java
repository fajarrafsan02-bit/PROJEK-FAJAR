package com.projek.tokweb.models.customer;

public enum PaymentStatus {
    PENDING("Menunggu"),
    PROCESSING("Sedang Diproses"),
    SUCCESS("Berhasil"),
    FAILED("Gagal"),
    EXPIRED("Kadaluarsa"),
    CANCELLED("Dibatalkan");
    
    private final String displayName;
    
    PaymentStatus(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public boolean isSuccessful() {
        return this == SUCCESS;
    }
    
    public boolean isPending() {
        return this == PENDING || this == PROCESSING;
    }
    
    public boolean isFinal() {
        return this == SUCCESS || this == FAILED || this == EXPIRED || this == CANCELLED;
    }
}