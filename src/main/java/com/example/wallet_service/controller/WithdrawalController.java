package com.example.wallet_service.controller;

import com.example.wallet_service.dto.TransactionResponse;
import com.example.wallet_service.dto.WithdrawalRequest;
import com.example.wallet_service.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/wallets/{userId}/withdrawals")
public class WithdrawalController {

    private final TransactionService transactionService;

    public WithdrawalController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping
    public ResponseEntity<TransactionResponse> withdraw(
            @PathVariable String userId,
            @RequestHeader("Correlation-Id") String correlationId,
            @Valid @RequestBody WithdrawalRequest request) {
        TransactionResponse response = transactionService.withdraw(userId, request.amount(), correlationId);
        return ResponseEntity.ok(response);
    }
}
