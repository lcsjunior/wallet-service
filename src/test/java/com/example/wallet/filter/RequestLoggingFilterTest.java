package com.example.wallet.filter;

import static com.example.wallet.constants.AppHeader.CORRELATION_ID_HEADER;
import static com.example.wallet.constants.AppHeader.IDEMPOTENCY_KEY_HEADER;
import static com.example.wallet.filter.RequestLoggingFilter.CORRELATION_ID_MDC_KEY;
import static com.example.wallet.filter.RequestLoggingFilter.IDEMPOTENCY_KEY_MDC_KEY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@ExtendWith(MockitoExtension.class)
class RequestLoggingFilterTest {

  private static final String CORRELATION_ID = "abc123def456-trace-999";

  private static final String IDEMPOTENCY_KEY = "123e4567-e89b-12d3-a456-426614174000";

  private static final String REQUEST_URI = "/v1/wallets";

  @Mock private FilterChain filterChain;

  private final RequestLoggingFilter filter = new RequestLoggingFilter();

  private final AtomicReference<Map<String, String>> mdcDuringChain = new AtomicReference<>();

  @Test
  @DisplayName(
      "Deve expor o correlation-id e o idempotency-key no MDC quando os headers são informados")
  void shouldExposeBothIdsWhenHeadersArePresent() throws Exception {
    var request = requestWithHeaders();
    captureMdcDuringChain();

    filter.doFilter(request, new MockHttpServletResponse(), filterChain);

    assertThat(mdcDuringChain.get())
        .containsEntry(CORRELATION_ID_MDC_KEY, CORRELATION_ID)
        .containsEntry(IDEMPOTENCY_KEY_MDC_KEY, IDEMPOTENCY_KEY);
    assertThat(MDC.get(CORRELATION_ID_MDC_KEY)).isNull();
    assertThat(MDC.get(IDEMPOTENCY_KEY_MDC_KEY)).isNull();
    verify(filterChain).doFilter(any(), any());
  }

  @Test
  @DisplayName("Deve expor apenas o idempotency-key no MDC quando o correlation-id não é informado")
  void shouldExposeIdempotencyKeyWhenCorrelationIdIsAbsent() throws Exception {
    var request = new MockHttpServletRequest("POST", REQUEST_URI);
    request.addHeader(IDEMPOTENCY_KEY_HEADER, IDEMPOTENCY_KEY);
    captureMdcDuringChain();

    filter.doFilter(request, new MockHttpServletResponse(), filterChain);

    assertThat(mdcDuringChain.get())
        .containsEntry(IDEMPOTENCY_KEY_MDC_KEY, IDEMPOTENCY_KEY)
        .doesNotContainKey(CORRELATION_ID_MDC_KEY);
  }

  @Test
  @DisplayName("Deve deixar o MDC vazio quando nenhum header é informado")
  void shouldLeaveMdcEmptyWhenHeadersAreAbsent() throws Exception {
    captureMdcDuringChain();

    filter.doFilter(
        new MockHttpServletRequest("POST", REQUEST_URI),
        new MockHttpServletResponse(),
        filterChain);

    assertThat(mdcDuringChain.get())
        .doesNotContainKeys(CORRELATION_ID_MDC_KEY, IDEMPOTENCY_KEY_MDC_KEY);
  }

  @Test
  @DisplayName("Deve limpar o MDC quando a requisição falha")
  void shouldClearMdcWhenChainThrows() throws Exception {
    var request = requestWithHeaders();
    doThrow(new ServletException()).when(filterChain).doFilter(any(), any());

    assertThatThrownBy(() -> filter.doFilter(request, new MockHttpServletResponse(), filterChain))
        .isInstanceOf(ServletException.class);

    assertThat(MDC.get(CORRELATION_ID_MDC_KEY)).isNull();
    assertThat(MDC.get(IDEMPOTENCY_KEY_MDC_KEY)).isNull();
  }

  private static MockHttpServletRequest requestWithHeaders() {
    var request = new MockHttpServletRequest("POST", REQUEST_URI);
    request.addHeader(CORRELATION_ID_HEADER, CORRELATION_ID);
    request.addHeader(IDEMPOTENCY_KEY_HEADER, IDEMPOTENCY_KEY);
    return request;
  }

  private void captureMdcDuringChain() throws Exception {
    doAnswer(
            invocation -> {
              var context = MDC.getCopyOfContextMap();
              mdcDuringChain.set(context == null ? new HashMap<>() : context);
              return null;
            })
        .when(filterChain)
        .doFilter(any(), any());
  }
}
