package com.example.wallet.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

class CacheConfigTest {

  private final ApplicationContextRunner contextRunner =
      new ApplicationContextRunner()
          .withBean(CacheManager.class, ConcurrentMapCacheManager::new)
          .withUserConfiguration(CacheConfig.class);

  @Test
  @DisplayName("should fail to start when the cache TTL is zero")
  void shouldFailToStartWhenTtlIsZero() {
    contextRunner
        .withPropertyValues("spring.cache.redis.time-to-live=0s")
        .run(context -> assertThat(context).hasFailed());
  }

  @Test
  @DisplayName("should fail to start when the cache TTL is negative")
  void shouldFailToStartWhenTtlIsNegative() {
    contextRunner
        .withPropertyValues("spring.cache.redis.time-to-live=-5s")
        .run(context -> assertThat(context).hasFailed());
  }

  @Test
  @DisplayName("should start normally when the cache TTL is a positive duration")
  void shouldStartWhenTtlIsPositive() {
    contextRunner
        .withPropertyValues("spring.cache.redis.time-to-live=1h")
        .run(context -> assertThat(context).hasNotFailed());
  }
}
