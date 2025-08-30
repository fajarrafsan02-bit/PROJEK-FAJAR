package com.projek.tokweb.enums;

public enum PaymentMethod {
    QR_CODE("QR Code"),
    BANK_TRANSFER("Transfer Bank");
    
    private final String displayName;
    
    PaymentMethod(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}
