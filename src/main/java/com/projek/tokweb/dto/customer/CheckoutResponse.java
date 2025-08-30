package com.projek.tokweb.dto.customer;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CheckoutResponse {
    private boolean success;
    private String message;
    private Map<String, Object> data;
}