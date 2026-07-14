package com.example.wallet_service.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateWalletRequest(@NotBlank String userId) {
}
