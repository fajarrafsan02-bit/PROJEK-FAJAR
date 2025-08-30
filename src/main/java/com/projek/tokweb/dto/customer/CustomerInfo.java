package com.projek.tokweb.dto.customer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerInfo {
    private String fullName;
    private String email;
    private String phone;
    private String birthDate;
    private String address;
    private String city;
    private String postalCode;
}