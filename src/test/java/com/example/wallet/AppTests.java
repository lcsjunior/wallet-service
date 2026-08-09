package com.example.wallet;

import com.example.wallet.repository.WalletRepository;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class AppTests {

  private static final int REDIS_PORT = 6379;

  @ServiceConnection
  static final GenericContainer<?> redis =
      new GenericContainer<>("redis:7-alpine").withExposedPorts(REDIS_PORT);

  static {
    redis.start();
  }

  @Autowired protected MockMvc mockMvc;

  @Autowired protected WalletRepository walletRepository;

  @Autowired private RedisConnectionFactory redisConnectionFactory;

  @BeforeEach
  void resetState() {
    try (var connection = redisConnectionFactory.getConnection()) {
      connection.serverCommands().flushAll();
    }
  }

  protected BigDecimal balanceOf(String walletId) {
    return walletRepository.findById(UUID.fromString(walletId)).orElseThrow().getBalance();
  }
}
