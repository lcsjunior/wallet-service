package com.example.wallet_service.controller;

import com.example.wallet_service.dto.TransferRequest;
import com.example.wallet_service.dto.TransferResponse;
import com.example.wallet_service.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/transfers")
public class TransferController {

    private final TransactionService transactionService;

    public TransferController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping
    public ResponseEntity<TransferResponse> transfer(
            @RequestHeader("Correlation-Id") String correlationId,
            @Valid @RequestBody TransferRequest request) {
        TransferResponse response = transactionService.transfer(request.fromUserId(), request.toUserId(),
                request.amount(), correlationId);
        return ResponseEntity.ok(response);
    }
}
