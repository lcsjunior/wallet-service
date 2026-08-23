package com.example.wallet.filter;

import static com.example.wallet.constants.Constants.CORRELATION_ID_HEADER;
import static com.example.wallet.constants.Constants.CORRELATION_ID_MDC_KEY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
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

  private static final String CORRELATION_ID = "3f1c9c1e-0d3a-4a6f-9a4a-1c2c9c0f5b21";

  private static final String REQUEST_URI = "/v1/wallets/1/deposits";

  @Mock private FilterChain filterChain;

  private final RequestLoggingFilter filter = new RequestLoggingFilter();

  private final AtomicReference<String> mdcDuringChain = new AtomicReference<>();

  @Test
  @DisplayName("Deve expor o correlation-id do header no MDC quando o header é informado")
  void shouldExposeHeaderCorrelationIdWhenHeaderIsPresent() throws Exception {
    var request = new MockHttpServletRequest("POST", REQUEST_URI);
    request.addHeader(CORRELATION_ID_HEADER, CORRELATION_ID);
    captureMdcDuringChain();

    filter.doFilter(request, new MockHttpServletResponse(), filterChain);

    assertThat(mdcDuringChain.get()).isEqualTo(CORRELATION_ID);
    assertThat(MDC.get(CORRELATION_ID_MDC_KEY)).isNull();
    verify(filterChain).doFilter(any(), any());
  }

  @Test
  @DisplayName("Deve deixar o MDC sem correlation-id quando o header não é informado")
  void shouldLeaveMdcEmptyWhenHeaderIsAbsent() throws Exception {
    captureMdcDuringChain();

    filter.doFilter(
        new MockHttpServletRequest("POST", REQUEST_URI),
        new MockHttpServletResponse(),
        filterChain);

    assertThat(mdcDuringChain.get()).isNull();
  }

  @Test
  @DisplayName("Deve limpar o MDC quando a requisição falha")
  void shouldClearMdcWhenChainThrows() throws Exception {
    var request = new MockHttpServletRequest("POST", REQUEST_URI);
    request.addHeader(CORRELATION_ID_HEADER, CORRELATION_ID);
    doThrow(new ServletException()).when(filterChain).doFilter(any(), any());

    assertThatThrownBy(() -> filter.doFilter(request, new MockHttpServletResponse(), filterChain))
        .isInstanceOf(ServletException.class);

    assertThat(MDC.get(CORRELATION_ID_MDC_KEY)).isNull();
  }

  private void captureMdcDuringChain() throws Exception {
    doAnswer(
            invocation -> {
              mdcDuringChain.set(MDC.get(CORRELATION_ID_MDC_KEY));
              return null;
            })
        .when(filterChain)
        .doFilter(any(), any());
  }
}
