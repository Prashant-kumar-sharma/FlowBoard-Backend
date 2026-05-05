package com.flowboard.workspace.dto.response;

import lombok.Data;

@Data
public class PaymentEntitlementResponse {
    private Long userId;
    private boolean premium;
    private Integer workspaceLimit;
    private Integer memberLimit;
    private String planCode;
}
