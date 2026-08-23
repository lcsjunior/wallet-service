package com.example.wallet;

import com.example.wallet.repository.WalletRepository;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class AppTests {

  @Autowired protected MockMvc mockMvc;

  @Autowired protected WalletRepository walletRepository;

  protected BigDecimal balanceOf(String walletId) {
    return walletRepository.findById(UUID.fromString(walletId)).orElseThrow().getBalance();
  }
}
