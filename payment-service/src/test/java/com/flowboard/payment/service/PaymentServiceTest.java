package com.flowboard.payment.service;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.flowboard.payment.dto.request.ConfirmPaymentRequest;
import com.flowboard.payment.dto.response.CheckoutSessionResponse;
import com.flowboard.payment.dto.response.PaymentEntitlementResponse;
import com.flowboard.payment.dto.response.PaymentSummaryResponse;
import com.flowboard.payment.entity.PaymentOrder;
import com.flowboard.payment.entity.PremiumSubscription;
import com.flowboard.payment.kafka.PaymentEventProducer;
import com.flowboard.payment.repository.PaymentOrderRepository;
import com.flowboard.payment.repository.PremiumSubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentOrderRepository paymentOrderRepository;
    @Mock
    private PremiumSubscriptionRepository premiumSubscriptionRepository;
    @Mock
    private RestClient razorpayRestClient;
    @Mock
    private PaymentEventProducer paymentEventProducer;
    @Mock
    private PaymentQueryService paymentQueryService;

    @InjectMocks
    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(paymentService, "premiumAmountPaise", 49900);
        ReflectionTestUtils.setField(paymentService, "currency", "INR");
        ReflectionTestUtils.setField(paymentService, "freeWorkspaceLimit", 5);
        ReflectionTestUtils.setField(paymentService, "freeMemberLimit", 5);
    }

    @Test
    void createCheckoutReturnsAlreadyActivePayloadForPremiumUser() {
        when(paymentQueryService.isPremiumUser(1L)).thenReturn(true);
        when(paymentQueryService.getRazorpayKeyId()).thenReturn("rzp_test");

        CheckoutSessionResponse response = paymentService.createCheckout(1L);

        assertThat(response.isPremiumAlreadyActive()).isTrue();
        assertThat(response.getProvider()).isEqualTo("RAZORPAY");
    }

    @Test
    void createCheckoutBuildsOrderForNonPremiumUser() throws Exception {
        when(paymentQueryService.isPremiumUser(1L)).thenReturn(false);
        when(paymentQueryService.getRazorpayKeyId()).thenReturn("rzp_test");
        when(paymentOrderRepository.save(any(PaymentOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        try (HttpServerContext server = startOrderServer("{\"id\":\"order_123\",\"amount\":49900,\"currency\":\"INR\"}")) {
            PaymentService service = createServiceWithRestClient(server.client());

            CheckoutSessionResponse response = service.createCheckout(1L);

            assertThat(response.getProviderOrderId()).isEqualTo("order_123");
            assertThat(response.getDisplayAmount()).isEqualTo("Rs. 499.00");
        }
    }

    @Test
    void createCheckoutThrowsWhenGatewayReturnsNoOrderId() throws Exception {
        when(paymentQueryService.isPremiumUser(1L)).thenReturn(false);

        try (HttpServerContext server = startOrderServer("{\"amount\":49900,\"currency\":\"INR\"}")) {
            PaymentService service = createServiceWithRestClient(server.client());

            assertThatThrownBy(() -> service.createCheckout(1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Failed to create Razorpay order");
        }
    }

    @Test
    void confirmPaymentReturnsSummaryWhenOrderAlreadyPaid() {
        ConfirmPaymentRequest request = new ConfirmPaymentRequest();
        request.setProviderOrderId("order_123");
        PaymentOrder order = PaymentOrder.builder()
                .userId(1L)
                .providerOrderId("order_123")
                .status(PaymentOrder.Status.PAID)
                .build();
        PaymentSummaryResponse summary = PaymentSummaryResponse.builder().userId(1L).premium(true).build();

        when(paymentOrderRepository.findByProviderOrderId("order_123")).thenReturn(Optional.of(order));
        when(paymentQueryService.getSummary(1L)).thenReturn(summary);

        assertThat(paymentService.confirmPayment(1L, request)).isSameAs(summary);
    }

    @Test
    void confirmPaymentRejectsOrdersFromAnotherUser() {
        ConfirmPaymentRequest request = new ConfirmPaymentRequest();
        request.setProviderOrderId("order_123");
        PaymentOrder order = PaymentOrder.builder()
                .userId(2L)
                .providerOrderId("order_123")
                .status(PaymentOrder.Status.CREATED)
                .build();

        when(paymentOrderRepository.findByProviderOrderId("order_123")).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> paymentService.confirmPayment(1L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not belong");
    }

    @Test
    void confirmPaymentMarksOrderFailedWhenSignatureInvalid() {
        ConfirmPaymentRequest request = new ConfirmPaymentRequest();
        request.setProviderOrderId("order_123");
        request.setProviderPaymentId("pay_123");
        request.setRazorpaySignature("bad");
        PaymentOrder order = PaymentOrder.builder()
                .userId(1L)
                .providerOrderId("order_123")
                .status(PaymentOrder.Status.CREATED)
                .build();

        when(paymentOrderRepository.findByProviderOrderId("order_123")).thenReturn(Optional.of(order));
        when(paymentQueryService.verifySignature("order_123", "pay_123", "bad")).thenReturn(false);

        assertThatThrownBy(() -> paymentService.confirmPayment(1L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("verification failed");
        assertThat(order.getStatus()).isEqualTo(PaymentOrder.Status.FAILED);
        verify(paymentOrderRepository).save(order);
    }

    @Test
    void confirmPaymentActivatesSubscriptionAndPublishesEvent() {
        ConfirmPaymentRequest request = new ConfirmPaymentRequest();
        request.setProviderOrderId("order_123");
        request.setProviderPaymentId("pay_123");
        request.setRazorpaySignature("good");

        PaymentOrder order = PaymentOrder.builder()
                .userId(1L)
                .providerOrderId("order_123")
                .providerName("RAZORPAY")
                .amountPaise(49900)
                .currency("INR")
                .status(PaymentOrder.Status.CREATED)
                .build();

        when(paymentOrderRepository.findByProviderOrderId("order_123")).thenReturn(Optional.of(order));
        when(paymentQueryService.verifySignature("order_123", "pay_123", "good")).thenReturn(true);
        when(premiumSubscriptionRepository.findByUserId(1L)).thenReturn(Optional.of(PremiumSubscription.builder().userId(1L).build()));
        when(paymentQueryService.getRazorpayKeyId()).thenReturn("rzp_test");

        PaymentSummaryResponse response = paymentService.confirmPayment(1L, request);

        assertThat(response.getUserId()).isEqualTo(1L);
        assertThat(response.isPremium()).isTrue();
        assertThat(response.getPlanCode()).isEqualTo("PREMIUM_MONTHLY");
        assertThat(response.getPlanName()).isEqualTo("FlowBoard Premium");
        assertThat(response.getPremiumAmountPaise()).isEqualTo(49900);
        assertThat(response.getCurrency()).isEqualTo("INR");
        assertThat(response.getRazorpayKeyId()).isEqualTo("rzp_test");
        assertThat(response.getActivatedAt()).isNotNull();
        assertThat(order.getStatus()).isEqualTo(PaymentOrder.Status.PAID);
        assertThat(order.getProviderPaymentId()).isEqualTo("pay_123");
        verify(paymentEventProducer).sendPremiumActivated(any(PaymentEventProducer.PremiumActivatedEvent.class));
    }

    @Test
    void confirmPaymentCreatesSubscriptionWhenMissing() {
        ConfirmPaymentRequest request = new ConfirmPaymentRequest();
        request.setProviderOrderId("order_123");
        request.setProviderPaymentId("pay_123");
        request.setRazorpaySignature("good");

        PaymentOrder order = PaymentOrder.builder()
                .userId(1L)
                .providerOrderId("order_123")
                .providerName("RAZORPAY")
                .amountPaise(49900)
                .currency("INR")
                .status(PaymentOrder.Status.CREATED)
                .build();

        when(paymentOrderRepository.findByProviderOrderId("order_123")).thenReturn(Optional.of(order));
        when(paymentQueryService.verifySignature("order_123", "pay_123", "good")).thenReturn(true);
        when(premiumSubscriptionRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(paymentQueryService.getRazorpayKeyId()).thenReturn("rzp_test");

        PaymentSummaryResponse response = paymentService.confirmPayment(1L, request);

        assertThat(response.getUserId()).isEqualTo(1L);
        assertThat(response.isPremium()).isTrue();
        assertThat(response.getPlanCode()).isEqualTo("PREMIUM_MONTHLY");
        verify(premiumSubscriptionRepository).save(any(PremiumSubscription.class));
    }

    @Test
    void getSummaryDelegatesToQueryService() {
        PaymentSummaryResponse summary = PaymentSummaryResponse.builder().userId(1L).build();
        when(paymentQueryService.getSummary(1L)).thenReturn(summary);

        assertThat(paymentService.getSummary(1L)).isSameAs(summary);
    }

    @Test
    void getEntitlementDelegatesToQueryService() {
        PaymentEntitlementResponse entitlement = PaymentEntitlementResponse.builder().userId(1L).build();
        when(paymentQueryService.getEntitlement(1L)).thenReturn(entitlement);

        assertThat(paymentService.getEntitlement(1L)).isSameAs(entitlement);
    }

    @Test
    void isPremiumUserDelegatesToQueryService() {
        when(paymentQueryService.isPremiumUser(1L)).thenReturn(true);

        assertThat(paymentService.isPremiumUser(1L)).isTrue();
    }

    @Test
    void deleteUserPaymentDataRemovesSubscriptionsAndOrders() {
        paymentService.deleteUserPaymentData(1L);

        verify(premiumSubscriptionRepository).deleteByUserId(1L);
        verify(paymentOrderRepository).deleteByUserId(1L);
    }

    private PaymentService createServiceWithRestClient(RestClient client) {
        PaymentService service = new PaymentService(
                paymentOrderRepository,
                premiumSubscriptionRepository,
                client,
                paymentEventProducer,
                paymentQueryService
        );
        ReflectionTestUtils.setField(service, "premiumAmountPaise", 49900);
        ReflectionTestUtils.setField(service, "currency", "INR");
        ReflectionTestUtils.setField(service, "freeWorkspaceLimit", 5);
        ReflectionTestUtils.setField(service, "freeMemberLimit", 5);
        return service;
    }

    private HttpServerContext startOrderServer(String responseBody) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/orders", new JsonHandler(responseBody));
        server.start();
        RestClient client = RestClient.builder()
                .baseUrl("http://localhost:" + server.getAddress().getPort())
                .build();
        return new HttpServerContext(server, client);
    }

    private record HttpServerContext(HttpServer server, RestClient client) implements AutoCloseable {
        @Override
        public void close() {
            server.stop(0);
        }
    }

    private record JsonHandler(String body) implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            byte[] bytes = body.getBytes();
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(bytes);
            }
        }
    }
}
