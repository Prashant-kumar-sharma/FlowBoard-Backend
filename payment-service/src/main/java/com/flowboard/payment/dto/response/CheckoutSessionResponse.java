package com.flowboard.payment.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CheckoutSessionResponse {
    private String provider;
    private String keyId;
    private String providerOrderId;
    private String planCode;
    private Integer amountPaise;
    private String currency;
    private String displayAmount;
    private String title;
    private String description;
    private String customerMessage;
    private boolean premiumAlreadyActive;
}
