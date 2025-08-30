package com.projek.tokweb.dto.customer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentWebhookRequest {
    private String id;

    private String externalId; 

    private String status;

    private Long amount;

    private String paymentMethod;
}
