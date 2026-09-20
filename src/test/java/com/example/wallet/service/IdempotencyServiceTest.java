package com.example.wallet.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class IdempotencyServiceTest {

  private static final String APPLICATION_NAME = "wallet-service";

  private static final Duration TTL = Duration.ofMinutes(10);

  private static final String REQUEST_URI =
      "/v1/wallets/2c9a1d4b-8e57-4f10-9b3a-5d61e0f27c48/deposits";

  private static final String IDEMPOTENCY_KEY = "3d7f5b62-91ae-4c08-8e23-7b4a0c6d5f19";

  private static final String REDIS_KEY =
      APPLICATION_NAME + ":idempotency:" + REQUEST_URI + ":" + IDEMPOTENCY_KEY;

  private static final String RESERVED = "reserved";

  @Mock private StringRedisTemplate redisTemplate;

  @Mock private ValueOperations<String, String> valueOperations;

  private IdempotencyService idempotencyService;

  @BeforeEach
  void setUp() {
    idempotencyService = new IdempotencyService(redisTemplate, APPLICATION_NAME, TTL);
  }

  @Test
  @DisplayName("Deve reservar a chave quando ela ainda não foi usada")
  void shouldReserveWhenKeyIsUnused() {
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    when(valueOperations.setIfAbsent(REDIS_KEY, RESERVED, TTL)).thenReturn(true);

    assertThat(idempotencyService.reserve(REQUEST_URI, IDEMPOTENCY_KEY)).isTrue();
  }

  @Test
  @DisplayName("Deve recusar a reserva quando a chave já está reservada")
  void shouldRejectWhenKeyIsAlreadyReserved() {
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    when(valueOperations.setIfAbsent(REDIS_KEY, RESERVED, TTL)).thenReturn(false);

    assertThat(idempotencyService.reserve(REQUEST_URI, IDEMPOTENCY_KEY)).isFalse();
  }

  @Test
  @DisplayName("Deve seguir para o banco quando o Redis está indisponível")
  void shouldReserveWhenRedisIsUnavailable() {
    when(redisTemplate.opsForValue())
        .thenThrow(new RedisConnectionFailureException("connection refused"));

    assertThat(idempotencyService.reserve(REQUEST_URI, IDEMPOTENCY_KEY)).isTrue();
  }
}
