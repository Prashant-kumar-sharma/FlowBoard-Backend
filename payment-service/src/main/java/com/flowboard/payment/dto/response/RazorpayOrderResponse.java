package com.flowboard.payment.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class RazorpayOrderResponse {
    private String id;
    private String entity;
    private Integer amount;
    @JsonProperty("amount_paid")
    private Integer amountPaid;
    @JsonProperty("amount_due")
    private Integer amountDue;
    private String currency;
    private String receipt;
    private String status;
}
