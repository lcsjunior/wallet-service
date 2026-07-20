package com.example.wallet_service.controller;

import com.example.wallet_service.dto.DepositRequest;
import com.example.wallet_service.dto.TransactionResponse;
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
@RequestMapping("/wallets/{userId}/deposits")
public class DepositController {

    private final TransactionService transactionService;

    public DepositController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping
    public ResponseEntity<TransactionResponse> deposit(
            @PathVariable String userId,
            @RequestHeader("Correlation-Id") String correlationId,
            @Valid @RequestBody DepositRequest request) {
        TransactionResponse response = transactionService.deposit(userId, request.amount(), correlationId);
        return ResponseEntity.ok(response);
    }
}
