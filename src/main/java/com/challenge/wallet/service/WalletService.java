package com.challenge.wallet.service;

import com.challenge.wallet.dto.BalanceResponse;
import com.challenge.wallet.dto.CreateWalletResponse;
import com.challenge.wallet.dto.DepositRequest;
import com.challenge.wallet.dto.HistoricalBalanceQuery;
import com.challenge.wallet.dto.HistoricalBalanceResponse;
import com.challenge.wallet.dto.TransferRequest;
import com.challenge.wallet.dto.WithdrawRequest;

import java.util.UUID;

public interface WalletService {

    CreateWalletResponse createWallet();

    BalanceResponse retrieveBalance(UUID walletId);

    HistoricalBalanceResponse retrieveHistoricalBalance(UUID walletId, HistoricalBalanceQuery query);

    void deposit(DepositRequest depositRequest);

    void withdraw(WithdrawRequest depositWithdraw);

    void transfer(TransferRequest transferWithdraw);
}
