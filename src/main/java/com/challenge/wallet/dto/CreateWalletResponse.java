package com.challenge.wallet.dto;

import com.challenge.wallet.model.Wallet;

import java.util.UUID;

public record CreateWalletResponse(
        UUID walletId) {

    public static CreateWalletResponse from(Wallet wallet) {
        return new CreateWalletResponse(wallet.getId());
    }
}
