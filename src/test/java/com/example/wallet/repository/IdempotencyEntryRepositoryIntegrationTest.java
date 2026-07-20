package com.example.wallet.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.wallet.entity.IdempotencyEntry;
import com.example.wallet.entity.OperationType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@SpringBootTest(properties = {"spring.cache.type=redis", "spring.autoconfigure.exclude="})
class IdempotencyEntryRepositoryIntegrationTest {

  @Container
  static final GenericContainer<?> redis =
      new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);

  @DynamicPropertySource
  static void redisProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.data.redis.host", redis::getHost);
    registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
  }

  @Autowired private IdempotencyEntryRepository idempotencyEntryRepository;

  @Test
  @DisplayName("should return the entry from cache without needing the database row present")
  void shouldReturnEntryFromCache() {
    var entry = IdempotencyEntry.of("cache-hit-correlation-id", OperationType.DEPOSIT, "fp", "{}");
    idempotencyEntryRepository.cache(entry);

    var found = idempotencyEntryRepository.findById(entry.getCorrelationId());

    assertThat(found).isPresent();
    assertThat(found.get().getCorrelationId()).isEqualTo(entry.getCorrelationId());
    assertThat(found.get().getRequestFingerprint()).isEqualTo(entry.getRequestFingerprint());
    assertThat(found.get().getResultBody()).isEqualTo(entry.getResultBody());
  }

  @Test
  @DisplayName("should fall through to the database on a cache miss and repopulate the cache")
  void shouldFallThroughToDatabaseOnCacheMiss() {
    var entry = IdempotencyEntry.of("cache-miss-correlation-id", OperationType.DEPOSIT, "fp", "{}");
    idempotencyEntryRepository.save(entry);

    var found = idempotencyEntryRepository.findById(entry.getCorrelationId());

    assertThat(found).isPresent();
    assertThat(found.get().getCorrelationId()).isEqualTo(entry.getCorrelationId());
  }

  @Test
  @DisplayName("should make a cached entry retrievable by a subsequent findById call")
  void shouldMakeCachedEntryRetrievable() {
    var entry = IdempotencyEntry.of("cache-put-correlation-id", OperationType.DEPOSIT, "fp", "{}");

    idempotencyEntryRepository.cache(entry);

    var found = idempotencyEntryRepository.findById(entry.getCorrelationId());

    assertThat(found).isPresent();
    assertThat(found.get().getCorrelationId()).isEqualTo(entry.getCorrelationId());
  }
}
