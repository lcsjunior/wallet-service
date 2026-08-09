package com.example.wallet.repository;

import static com.example.wallet.constants.Messages.WALLET_NOT_FOUND;
import static org.springframework.http.HttpStatus.NOT_FOUND;

import com.example.wallet.entity.Wallet;
import com.example.wallet.exception.ServiceException;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WalletRepository extends JpaRepository<Wallet, UUID> {

  default Wallet findWallet(UUID walletId) {
    return findById(walletId).orElseThrow(() -> ServiceException.of(WALLET_NOT_FOUND, NOT_FOUND));
  }
}
