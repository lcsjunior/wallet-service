package com.example.wallet.controller;

import static com.example.wallet.constants.Constants.IDEMPOTENCY_KEY_HEADER;

import com.example.wallet.dto.WithdrawalRequest;
import com.example.wallet.service.TransactionService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/wallets/{walletId}/withdrawals")
public class WithdrawalController {

  private final TransactionService transactionService;

  public WithdrawalController(TransactionService transactionService) {
    this.transactionService = transactionService;
  }

  @PostMapping
  public ResponseEntity<Void> withdraw(
      @PathVariable UUID walletId,
      @RequestHeader(IDEMPOTENCY_KEY_HEADER) UUID idempotencyKey,
      @Valid @RequestBody WithdrawalRequest request) {
    transactionService.withdraw(walletId, request.amount(), idempotencyKey);
    return ResponseEntity.noContent().build();
  }
}
