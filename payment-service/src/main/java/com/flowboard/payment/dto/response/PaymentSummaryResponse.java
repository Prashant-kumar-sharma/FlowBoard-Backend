package com.flowboard.payment.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class PaymentSummaryResponse {
    private Long userId;
    private boolean premium;
    private String planCode;
    private String planName;
    private Integer workspaceLimit;
    private Integer memberLimit;
    private Integer premiumAmountPaise;
    private String currency;
    private String razorpayKeyId;
    private LocalDateTime activatedAt;
}
