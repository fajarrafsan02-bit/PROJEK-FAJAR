package com.projek.tokweb.dto.customer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentInstruction {
    private String externalId;
    private Double amount;
    private String paymentMethod;
    private String qrCode; // Base64 QR code untuk QR payment
    private String bankAccountNumber; // Nomor rekening untuk transfer bank
    private String bankName; // Nama bank
    private String paymentStatus;
    private String instructions;
}