package com.flowboard.payment.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ConfirmPaymentRequest {
    @NotBlank
    private String providerOrderId;

    @NotBlank
    private String providerPaymentId;

    private String razorpaySignature;
}
