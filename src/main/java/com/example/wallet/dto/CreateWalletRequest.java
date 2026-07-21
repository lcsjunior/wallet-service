package com.example.wallet.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateWalletRequest(@NotNull(message = "{userid.notblank}") UUID userId) {}
