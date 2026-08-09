package com.example.wallet.service;

import com.example.wallet.dto.BalanceResponse;
import com.example.wallet.dto.WalletResponse;
import com.example.wallet.entity.Wallet;
import com.example.wallet.mapper.WalletMapper;
import com.example.wallet.repository.WalletRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WalletService {

  private final WalletRepository walletRepository;
  private final WalletMapper walletMapper;

  public WalletService(WalletRepository walletRepository, WalletMapper walletMapper) {
    this.walletRepository = walletRepository;
    this.walletMapper = walletMapper;
  }

  @Transactional
  public WalletResponse createWallet(UUID userId) {
    var wallet = Wallet.of(userId);
    walletRepository.save(wallet);
    return walletMapper.toWalletResponse(wallet);
  }

  public BalanceResponse getCurrentBalance(UUID walletId) {
    var wallet = walletRepository.findWallet(walletId);
    return walletMapper.toBalanceResponse(wallet);
  }
}
