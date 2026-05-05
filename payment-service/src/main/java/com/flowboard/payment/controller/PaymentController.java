package com.flowboard.payment.controller;

import com.flowboard.payment.dto.request.ConfirmPaymentRequest;
import com.flowboard.payment.dto.response.CheckoutSessionResponse;
import com.flowboard.payment.dto.response.PaymentSummaryResponse;
import com.flowboard.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Premium membership and Razorpay checkout")
@SecurityRequirement(name = "bearerAuth")
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping("/summary")
    @Operation(summary = "Get current user premium summary")
    public ResponseEntity<PaymentSummaryResponse> getSummary(@RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(paymentService.getSummary(userId));
    }

    @PostMapping("/checkout")
    @Operation(summary = "Create a Razorpay checkout order")
    public ResponseEntity<CheckoutSessionResponse> createCheckout(@RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(paymentService.createCheckout(userId));
    }

    @PostMapping("/confirm")
    @Operation(summary = "Verify Razorpay payment and activate premium")
    public ResponseEntity<PaymentSummaryResponse> confirmPayment(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody ConfirmPaymentRequest request) {
        return ResponseEntity.ok(paymentService.confirmPayment(userId, request));
    }
}
