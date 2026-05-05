package com.flowboard.payment.controller;

import com.flowboard.payment.dto.response.PaymentEntitlementResponse;
import com.flowboard.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Hidden
@RestController
@RequestMapping("/api/v1/internal/payments")
@RequiredArgsConstructor
public class InternalPaymentController {

    private final PaymentService paymentService;

    @GetMapping("/users/{userId}/entitlement")
    public ResponseEntity<PaymentEntitlementResponse> getEntitlement(@PathVariable Long userId) {
        return ResponseEntity.ok(paymentService.getEntitlement(userId));
    }

    @DeleteMapping("/users/{userId}")
    public ResponseEntity<Void> deleteUserPaymentData(@PathVariable Long userId) {
        paymentService.deleteUserPaymentData(userId);
        return ResponseEntity.noContent().build();
    }
}
