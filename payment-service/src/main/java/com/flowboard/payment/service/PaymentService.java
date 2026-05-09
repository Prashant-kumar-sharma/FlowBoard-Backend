package com.flowboard.payment.service;

import com.flowboard.payment.dto.request.ConfirmPaymentRequest;
import com.flowboard.payment.dto.response.CheckoutSessionResponse;
import com.flowboard.payment.dto.response.PaymentEntitlementResponse;
import com.flowboard.payment.dto.response.PaymentSummaryResponse;
import com.flowboard.payment.dto.response.RazorpayOrderResponse;
import com.flowboard.payment.entity.PaymentOrder;
import com.flowboard.payment.entity.PremiumSubscription;
import com.flowboard.payment.kafka.PaymentEventProducer;
import com.flowboard.payment.repository.PaymentOrderRepository;
import com.flowboard.payment.repository.PremiumSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class PaymentService {

    private static final String PREMIUM_PLAN_CODE = "PREMIUM_MONTHLY";
    private static final String PREMIUM_PLAN_NAME = "FlowBoard Premium";
    private static final String RAZORPAY_PROVIDER = "RAZORPAY";

    private final PaymentOrderRepository paymentOrderRepository;
    private final PremiumSubscriptionRepository premiumSubscriptionRepository;
    private final RestClient razorpayRestClient;
    private final PaymentEventProducer paymentEventProducer;
    private final PaymentQueryService paymentQueryService;

    @Value("${payment.plan.premium-amount-paise}")
    private Integer premiumAmountPaise;

    @Value("${payment.plan.currency}")
    private String currency;

    @Value("${payment.plan.workspace-limit}")
    private Integer freeWorkspaceLimit;

    @Value("${payment.plan.member-limit}")
    private Integer freeMemberLimit;

    public PaymentSummaryResponse getSummary(Long userId) {
        return paymentQueryService.getSummary(userId);
    }

    public CheckoutSessionResponse createCheckout(Long userId) {
        if (paymentQueryService.isPremiumUser(userId)) {
            return CheckoutSessionResponse.builder()
                    .provider(RAZORPAY_PROVIDER)
                    .keyId(paymentQueryService.getRazorpayKeyId())
                    .providerOrderId("")
                    .planCode(PREMIUM_PLAN_CODE)
                    .amountPaise(premiumAmountPaise)
                    .currency(currency)
                    .displayAmount(formatAmount(premiumAmountPaise))
                    .title("Premium already active")
                    .description("This account already has premium access.")
                    .customerMessage("Your account already has unlimited workspaces and workspace members.")
                    .premiumAlreadyActive(true)
                    .build();
        }

        String receipt = "premium_" + userId + "_" + UUID.randomUUID().toString().substring(0, 8);
        RazorpayOrderResponse razorpayOrder = razorpayRestClient.post()
                .uri("/orders")
                .body(buildOrderRequest(receipt))
                .retrieve()
                .body(RazorpayOrderResponse.class);

        if (razorpayOrder == null || razorpayOrder.getId() == null) {
            throw new IllegalArgumentException("Failed to create Razorpay order");
        }

        PaymentOrder order = paymentOrderRepository.save(PaymentOrder.builder()
                .userId(userId)
                .providerOrderId(razorpayOrder.getId())
                .providerName(RAZORPAY_PROVIDER)
                .planCode(PREMIUM_PLAN_CODE)
                .amountPaise(razorpayOrder.getAmount())
                .currency(razorpayOrder.getCurrency())
                .status(PaymentOrder.Status.CREATED)
                .notes("Razorpay order receipt: " + receipt)
                .build());

        return CheckoutSessionResponse.builder()
                .provider(RAZORPAY_PROVIDER)
                .keyId(paymentQueryService.getRazorpayKeyId())
                .providerOrderId(order.getProviderOrderId())
                .planCode(PREMIUM_PLAN_CODE)
                .amountPaise(order.getAmountPaise())
                .currency(order.getCurrency())
                .displayAmount(formatAmount(order.getAmountPaise()))
                .title(PREMIUM_PLAN_NAME)
                .description("Unlock unlimited workspaces and invite more than five members into any workspace.")
                .customerMessage("This uses Razorpay test mode. No real money will be charged while you use test keys.")
                .premiumAlreadyActive(false)
                .build();
    }

    @CacheEvict(cacheNames = {"payment:summary", "payment:entitlement", "payment:premium"}, key = "#userId")
    public PaymentSummaryResponse confirmPayment(Long userId, ConfirmPaymentRequest request) {
        PaymentOrder order = paymentOrderRepository.findByProviderOrderId(request.getProviderOrderId())
                .orElseThrow(() -> new IllegalArgumentException("Payment order not found"));

        if (!order.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Payment order does not belong to this user");
        }

        if (order.getStatus() == PaymentOrder.Status.PAID) {
            return paymentQueryService.getSummary(userId);
        }

        if (!paymentQueryService.verifySignature(order.getProviderOrderId(), request.getProviderPaymentId(), request.getRazorpaySignature())) {
            order.setStatus(PaymentOrder.Status.FAILED);
            paymentOrderRepository.save(order);
            throw new IllegalArgumentException("Razorpay signature verification failed");
        }

        order.setStatus(PaymentOrder.Status.PAID);
        order.setProviderPaymentId(request.getProviderPaymentId());
        paymentOrderRepository.save(order);

        PremiumSubscription subscription = premiumSubscriptionRepository.findByUserId(userId)
                .orElse(PremiumSubscription.builder()
                        .userId(userId)
                        .planCode(PREMIUM_PLAN_CODE)
                        .providerName(RAZORPAY_PROVIDER)
                        .build());
        subscription.setStatus(PremiumSubscription.Status.ACTIVE);
        subscription.setPlanCode(PREMIUM_PLAN_CODE);
        subscription.setProviderOrderId(order.getProviderOrderId());
        subscription.setProviderPaymentId(order.getProviderPaymentId());
        subscription.setActivatedAt(LocalDateTime.now());
        premiumSubscriptionRepository.save(subscription);
        paymentEventProducer.sendPremiumActivated(new PaymentEventProducer.PremiumActivatedEvent(
                userId,
                PREMIUM_PLAN_CODE,
                PREMIUM_PLAN_NAME,
                order.getAmountPaise(),
                order.getCurrency(),
                order.getProviderName(),
                order.getProviderOrderId(),
                order.getProviderPaymentId(),
                subscription.getActivatedAt()
        ));

        return PaymentSummaryResponse.builder()
                .userId(userId)
                .premium(true)
                .planCode(PREMIUM_PLAN_CODE)
                .planName(PREMIUM_PLAN_NAME)
                .workspaceLimit(null)
                .memberLimit(null)
                .premiumAmountPaise(order.getAmountPaise())
                .currency(order.getCurrency())
                .razorpayKeyId(paymentQueryService.getRazorpayKeyId())
                .activatedAt(subscription.getActivatedAt())
                .build();
    }

    public PaymentEntitlementResponse getEntitlement(Long userId) {
        return paymentQueryService.getEntitlement(userId);
    }

    public boolean isPremiumUser(Long userId) {
        return paymentQueryService.isPremiumUser(userId);
    }

    @CacheEvict(cacheNames = {"payment:summary", "payment:entitlement", "payment:premium"}, key = "#userId")
    public void deleteUserPaymentData(Long userId) {
        premiumSubscriptionRepository.deleteByUserId(userId);
        paymentOrderRepository.deleteByUserId(userId);
    }

    private Map<String, Object> buildOrderRequest(String receipt) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("amount", premiumAmountPaise);
        payload.put("currency", currency);
        payload.put("receipt", receipt);
        Map<String, Object> notes = new LinkedHashMap<>();
        notes.put("plan", PREMIUM_PLAN_CODE);
        notes.put("product", PREMIUM_PLAN_NAME);
        payload.put("notes", notes);
        return payload;
    }

    private String formatAmount(Integer amountPaise) {
        return "Rs. " + new DecimalFormat("0.00").format(amountPaise / 100.0);
    }
}
