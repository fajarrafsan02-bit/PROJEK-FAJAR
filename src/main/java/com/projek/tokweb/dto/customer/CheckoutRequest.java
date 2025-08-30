package com.projek.tokweb.dto.customer;

import jakarta.validation.constraints.*;
import lombok.*;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class CheckoutRequest {

    @NotNull @Size(min = 1)
    private List<Item> items;

    @NotBlank
    private String paymentMethod;

    // Tambahkan field customerInfo sesuai yang dikirim frontend
    private CustomerInfo customerInfo;
    
    // Tambahkan field totalAmount yang dikirim frontend
    private Double totalAmount;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class Item {
        @NotNull
        private Long productId;
        @NotNull
        @Min(1)
        private Integer quantity;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class CustomerInfo {
        @NotBlank
        private String fullName;
        
        @Email @NotBlank
        private String email;
        
        @NotBlank
        private String phone;
        
        @NotBlank
        private String birthDate;
        
        @NotBlank
        private String address;
        
        @NotBlank
        private String city;
        
        @NotBlank
        private String postalCode;
    }
}
