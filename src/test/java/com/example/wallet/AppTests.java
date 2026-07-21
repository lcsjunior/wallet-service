package com.example.wallet;

import static com.example.wallet.utils.JsonUtils.fieldJson;
import static org.springframework.test.json.JsonCompareMode.STRICT;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

  @Autowired private RedisConnectionFactory redisConnectionFactory;

  @BeforeEach
  void flushCache() {
    redisConnectionFactory.getConnection().serverCommands().flushAll();
  }

  protected void expectBalance(String walletId, String balance) throws Exception {
    mockMvc
        .perform(get("/v1/wallets/" + walletId + "/balance"))
        .andExpect(status().isOk())
        .andExpect(content().json(fieldJson("balance", balance), STRICT));
  }
}
