package com.example.wallet.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateWalletRequest(@NotNull(message = "{userId.notblank}") UUID userId) {}
