package com.challenge.wallet.service;

import com.challenge.wallet.dto.BalanceResponse;
import com.challenge.wallet.dto.CreateWalletResponse;
import com.challenge.wallet.model.Wallet;

import java.util.UUID;

public interface WalletService {

    CreateWalletResponse createWallet();

    BalanceResponse getWallet(UUID walletId);
}
