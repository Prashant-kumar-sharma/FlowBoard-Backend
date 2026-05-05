package com.flowboard.workspace.service;

import com.flowboard.workspace.dto.response.PaymentEntitlementResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentEntitlementClientTest {

    @Mock
    private RestClient paymentRestClient;
    @Mock
    private RestClient.RequestHeadersUriSpec requestHeadersUriSpec;
    @Mock
    private RestClient.RequestHeadersSpec requestHeadersSpec;
    @Mock
    private RestClient.ResponseSpec responseSpec;

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void isPremiumReturnsTrueWhenPaymentServiceSaysPremium() {
        PaymentEntitlementResponse response = new PaymentEntitlementResponse();
        response.setPremium(true);
        PaymentEntitlementClient client = new PaymentEntitlementClient(paymentRestClient);

        doReturn(requestHeadersUriSpec).when(paymentRestClient).get();
        when(requestHeadersUriSpec.uri("/users/{userId}/entitlement", 1L)).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(PaymentEntitlementResponse.class)).thenReturn(response);

        assertThat(client.isPremium(1L)).isTrue();
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void isPremiumFallsBackToFalseWhenLookupFails() {
        PaymentEntitlementClient client = new PaymentEntitlementClient(paymentRestClient);

        doReturn(requestHeadersUriSpec).when(paymentRestClient).get();
        when(requestHeadersUriSpec.uri("/users/{userId}/entitlement", 1L)).thenThrow(new RuntimeException("down"));

        assertThat(client.isPremium(1L)).isFalse();
    }
}
