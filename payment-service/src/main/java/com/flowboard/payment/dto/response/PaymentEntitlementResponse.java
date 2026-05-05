package com.flowboard.payment.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaymentEntitlementResponse {
    private Long userId;
    private boolean premium;
    private Integer workspaceLimit;
    private Integer memberLimit;
    private String planCode;
}
