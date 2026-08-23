package com.example.wallet.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.wallet.dto.WalletResponse;
import com.example.wallet.entity.Wallet;
import com.example.wallet.mapper.WalletMapper;
import com.example.wallet.repository.WalletRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

  private static final UUID USER_ID = UUID.fromString("1a1b3c93-6d2f-4f7e-9a41-6c0f2a1d8e33");

  private static final UUID WALLET_ID = UUID.fromString("2c9a1d4b-8e57-4f10-9b3a-5d61e0f27c48");

  @Mock private WalletRepository walletRepository;

  @Mock private WalletMapper walletMapper;

  @InjectMocks private WalletService walletService;

  @Test
  @DisplayName("Deve persistir a carteira e devolver a resposta mapeada quando o usuário é válido")
  void shouldReturnMappedWalletWhenUserIsValid() {
    var walletResponse = new WalletResponse(WALLET_ID, Instant.EPOCH);
    when(walletMapper.toWalletResponse(any(Wallet.class))).thenReturn(walletResponse);

    assertThat(walletService.createWallet(USER_ID)).isEqualTo(walletResponse);

    verify(walletRepository).saveAndFlush(any(Wallet.class));
  }
}
