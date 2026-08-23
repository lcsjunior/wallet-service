package com.example.wallet.dto;

import java.time.Instant;
import java.util.UUID;

public record WalletResponse(UUID walletId, Instant createdAt) {}
