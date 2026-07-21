package com.example.wallet.controller;

import com.example.wallet.dto.TransferRequest;
import com.example.wallet.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/transfers")
public class TransferController {

  private final TransactionService transactionService;

  public TransferController(TransactionService transactionService) {
    this.transactionService = transactionService;
  }

  @PostMapping
  public ResponseEntity<Void> transfer(
      @RequestHeader("Correlation-Id") String correlationId,
      @Valid @RequestBody TransferRequest request) {
    transactionService.transfer(
        request.fromWalletId(), request.toWalletId(), request.amount(), correlationId);
    return ResponseEntity.noContent().build();
  }
}
