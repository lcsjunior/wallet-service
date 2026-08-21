package com.example.wallet.controller;

import static com.example.wallet.constants.Constants.CORRELATION_ID_HEADER;
import static com.example.wallet.constants.Constants.IDEMPOTENT_REPLAYED_HEADER;

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
      @RequestHeader(CORRELATION_ID_HEADER) String correlationId,
      @Valid @RequestBody TransferRequest request) {
    var outcome =
        transactionService.transfer(
            request.fromWalletId(), request.toWalletId(), request.amount(), correlationId);
    return ResponseEntity.noContent()
        .header(IDEMPOTENT_REPLAYED_HEADER, String.valueOf(outcome.isReplayed()))
        .build();
  }
}
