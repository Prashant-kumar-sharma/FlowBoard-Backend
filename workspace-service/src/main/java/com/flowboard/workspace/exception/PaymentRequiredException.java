package com.flowboard.workspace.exception;

import lombok.Getter;

@Getter
public class PaymentRequiredException extends RuntimeException {
    private final String limitType;
    private final int limit;
    private final long currentUsage;
    private final String upgradePath;
    private final Long ownerId;
    private final Long workspaceId;

    public PaymentRequiredException(String message, String limitType, int limit, long currentUsage, String upgradePath, Long ownerId, Long workspaceId) {
        super(message);
        this.limitType = limitType;
        this.limit = limit;
        this.currentUsage = currentUsage;
        this.upgradePath = upgradePath;
        this.ownerId = ownerId;
        this.workspaceId = workspaceId;
    }
}
