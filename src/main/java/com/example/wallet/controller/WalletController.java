package com.example.wallet.controller;

import static com.example.wallet.constants.AppHeader.IDEMPOTENCY_KEY_HEADER;
import static org.springframework.http.HttpStatus.CREATED;

import com.example.wallet.dto.CreateWalletRequest;
import com.example.wallet.dto.WalletResponse;
import com.example.wallet.service.WalletService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/wallets")
public class WalletController {

  private final WalletService walletService;

  public WalletController(WalletService walletService) {
    this.walletService = walletService;
  }

  @PostMapping
  public ResponseEntity<WalletResponse> createWallet(
      @RequestHeader(IDEMPOTENCY_KEY_HEADER) UUID idempotencyKey,
      @Valid @RequestBody CreateWalletRequest request) {
    var response = walletService.createWallet(request.userId(), idempotencyKey);
    return ResponseEntity.status(CREATED).body(response);
  }
}
