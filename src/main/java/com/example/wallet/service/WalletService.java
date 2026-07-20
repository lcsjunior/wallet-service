package com.example.wallet.service;

import static com.example.wallet.exception.ErrorCode.WALLET_NOT_FOUND;

import com.example.wallet.dto.BalanceResponse;
import com.example.wallet.dto.WalletResponse;
import com.example.wallet.entity.Wallet;
import com.example.wallet.exception.ServiceException;
import com.example.wallet.mapper.WalletMapper;
import com.example.wallet.repository.WalletRepository;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WalletService {

  private static final String LOG_PREFIX = "[WALLET_SERVICE] ";

  private static final Logger log = LoggerFactory.getLogger(WalletService.class);

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
    log.info(LOG_PREFIX + "Wallet created | walletId={}, userId={}", wallet.getId(), userId);
    return walletMapper.toWalletResponse(wallet);
  }

  @Transactional(readOnly = true)
  public BalanceResponse getCurrentBalance(UUID walletId) {
    var wallet = findByIdOrThrow(walletId);
    return new BalanceResponse(wallet.getId(), wallet.getBalance());
  }

  @Transactional(readOnly = true)
  public Wallet findByIdOrThrow(UUID walletId) {
    return walletRepository.findById(walletId).orElseThrow(() -> walletNotFoundException(walletId));
  }

  private ServiceException walletNotFoundException(UUID walletId) {
    log.warn(LOG_PREFIX + "Wallet not found | walletId={}", walletId);
    return new ServiceException(WALLET_NOT_FOUND);
  }
}
