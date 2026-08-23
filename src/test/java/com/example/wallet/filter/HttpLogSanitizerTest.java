package com.example.wallet.filter;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.wallet.config.HttpLoggingProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.event.Level;
import org.springframework.http.HttpHeaders;

class HttpLogSanitizerTest {

  private static final String REPLACEMENT = "***";

  private final HttpLoggingProperties properties =
      new HttpLoggingProperties(
          Level.INFO,
          4096,
          REPLACEMENT,
          Set.of("authorization", "cookie"),
          Set.of("password", "token"),
          List.of());

  private final HttpLogSanitizer sanitizer = new HttpLogSanitizer(properties, new ObjectMapper());

  @Test
  @DisplayName("Deve mascarar o header sensível e preservar os demais quando formata os headers")
  void shouldMaskHeaderWhenHeaderIsSensitive() {
    var headers = new HttpHeaders();
    headers.add(HttpHeaders.AUTHORIZATION, "Bearer segredo");
    headers.add(HttpHeaders.CONTENT_TYPE, "application/json");

    var formatted = sanitizer.headers(headers);

    assertThat(formatted).contains(REPLACEMENT).doesNotContain("segredo");
    assertThat(formatted).contains("application/json");
  }

  @Test
  @DisplayName("Deve mascarar o campo sensível aninhado quando o body é JSON")
  void shouldMaskNestedFieldWhenBodyIsJson() {
    var body =
        "{\"userId\":\"u-1\",\"credentials\":{\"password\":\"p\"},\"items\":[{\"token\":\"t\"}]}";

    var sanitized = sanitizer.body(body.getBytes(StandardCharsets.UTF_8));

    assertThat(sanitized).contains("u-1").doesNotContain("\"p\"").doesNotContain("\"t\"");
    assertThat(sanitized).contains(REPLACEMENT);
  }

  @Test
  @DisplayName("Deve omitir o body quando o JSON é malformado")
  void shouldOmitBodyWhenJsonIsMalformed() {
    var sanitized = sanitizer.body("{\"password\":".getBytes(StandardCharsets.UTF_8));

    assertThat(sanitized).isEqualTo("<unparseable body>");
  }

  @Test
  @DisplayName("Deve devolver vazio quando o body não foi lido")
  void shouldReturnEmptyWhenBodyIsEmpty() {
    assertThat(sanitizer.body(new byte[0])).isEmpty();
  }

  @Test
  @DisplayName("Deve truncar o body quando ele passa do limite configurado")
  void shouldTruncateBodyWhenItExceedsTheLimit() {
    var shortLimit =
        new HttpLoggingProperties(Level.INFO, 10, REPLACEMENT, Set.of(), Set.of(), List.of());
    var body = "{\"id\":\"0123456789abcdefghij\"}";

    var sanitized =
        new HttpLogSanitizer(shortLimit, new ObjectMapper())
            .body(body.getBytes(StandardCharsets.UTF_8));

    assertThat(sanitized).doesNotContain("abcdefghij");
  }
}
