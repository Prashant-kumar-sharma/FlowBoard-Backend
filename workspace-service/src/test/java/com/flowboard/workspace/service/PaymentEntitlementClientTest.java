package com.flowboard.workspace.service;

import com.flowboard.workspace.dto.response.PaymentEntitlementResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentEntitlementClientTest {

    @Mock
    private PaymentEntitlementApiClient paymentEntitlementApiClient;

    @Test
    void isPremiumReturnsTrueWhenPaymentServiceSaysPremium() {
        PaymentEntitlementResponse response = new PaymentEntitlementResponse();
        response.setPremium(true);
        PaymentEntitlementClient client = new PaymentEntitlementClient(paymentEntitlementApiClient);

        when(paymentEntitlementApiClient.getEntitlement(1L)).thenReturn(response);

        assertThat(client.isPremium(1L)).isTrue();
    }

    @Test
    void isPremiumFallsBackToFalseWhenLookupFails() {
        PaymentEntitlementClient client = new PaymentEntitlementClient(paymentEntitlementApiClient);

        when(paymentEntitlementApiClient.getEntitlement(1L)).thenThrow(new RuntimeException("down"));

        assertThat(client.isPremium(1L)).isFalse();
    }
}
