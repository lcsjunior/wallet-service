package com.example.wallet.filter;

import static com.example.wallet.constants.Constants.CORRELATION_ID_HEADER;
import static com.example.wallet.constants.Constants.CORRELATION_ID_MDC_KEY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.example.wallet.config.HttpLoggingProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.slf4j.event.Level;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@ExtendWith(MockitoExtension.class)
class HttpLoggingFilterTest {

  private static final String CORRELATION_ID = "3f1c9c1e-0d3a-4a6f-9a4a-1c2c9c0f5b21";
  private static final String REQUEST_URI = "/v1/wallets";
  private static final String EXCLUDED_URI = "/actuator/health";
  private static final String REQUEST_BODY = "{\"userId\":\"u-1\",\"password\":\"p\"}";
  private static final String RESPONSE_BODY = "{\"walletId\":\"w-1\"}";
  private static final String REPLACEMENT = "***";

  @Mock private FilterChain filterChain;

  private final ListAppender<ILoggingEvent> appender = new ListAppender<>();

  private final HttpLoggingProperties properties =
      new HttpLoggingProperties(
          Level.INFO,
          4096,
          REPLACEMENT,
          Set.of("authorization"),
          Set.of("password"),
          List.of("/actuator/**"));

  private final AtomicReference<Integer> entriesDuringChain = new AtomicReference<>();

  private final HttpLoggingFilter filter =
      new HttpLoggingFilter(properties, new HttpLogSanitizer(properties, new ObjectMapper()));

  @BeforeEach
  void startAppender() {
    appender.start();
    filterLogger().addAppender(appender);
  }

  @AfterEach
  void stopAppender() {
    filterLogger().detachAppender(appender);
    appender.stop();
  }

  @Test
  @DisplayName("Deve logar entrada e saída mascaradas quando a requisição termina")
  void shouldLogIncomingAndOutgoingWhenRequestCompletes() throws Exception {
    var request = jsonRequest();
    request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer segredo");
    var response = new MockHttpServletResponse();
    respondWithBody();

    filter.doFilter(request, response, filterChain);

    assertThat(response.getContentAsString()).isEqualTo(RESPONSE_BODY);
    assertThat(entriesDuringChain.get()).isOne();
    assertThat(appender.list).hasSize(2);
    assertThat(incomingMessage())
        .contains("HTTP INCOMING POST " + REQUEST_URI)
        .contains("\"userId\":\"u-1\"")
        .contains(REPLACEMENT)
        .doesNotContain("segredo")
        .doesNotContain("\"p\"");
    assertThat(outgoingMessage())
        .contains("HTTP OUTGOING")
        .contains("status=200")
        .contains("durationMs=")
        .contains("\"walletId\":\"w-1\"");
  }

  @Test
  @DisplayName("Deve registrar <not logged> quando o body não é JSON e preservar o stream")
  void shouldNotBufferBodyWhenContentTypeIsNotJson() throws Exception {
    var request = new MockHttpServletRequest("POST", REQUEST_URI);
    request.setContentType(TEXT_PLAIN_VALUE);
    request.setContent("texto".getBytes(StandardCharsets.UTF_8));
    var bodyReadByChain = new AtomicReference<String>();
    doAnswer(
            invocation -> {
              var chainRequest = (HttpServletRequest) invocation.getArgument(0);
              bodyReadByChain.set(
                  new String(chainRequest.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
              return null;
            })
        .when(filterChain)
        .doFilter(any(), any());

    filter.doFilter(request, new MockHttpServletResponse(), filterChain);

    assertThat(bodyReadByChain.get()).isEqualTo("texto");
    assertThat(incomingMessage()).contains("body=<not logged>");
  }

  @Test
  @DisplayName("Deve manter o body legível para o controller quando a requisição é envolvida")
  void shouldKeepBodyReadableWhenRequestIsWrapped() throws Exception {
    var bodyReadByChain = new AtomicReference<String>();
    doAnswer(
            invocation -> {
              var chainRequest = (HttpServletRequest) invocation.getArgument(0);
              bodyReadByChain.set(
                  new String(chainRequest.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
              return null;
            })
        .when(filterChain)
        .doFilter(any(), any());

    filter.doFilter(jsonRequest(), new MockHttpServletResponse(), filterChain);

    assertThat(bodyReadByChain.get()).isEqualTo(REQUEST_BODY);
  }

  @Test
  @DisplayName("Deve expor o correlation-id no MDC durante a cadeia e limpá-lo ao final")
  void shouldExposeCorrelationIdWhenHeaderIsPresent() throws Exception {
    var mdcDuringChain = new AtomicReference<String>();
    doAnswer(
            invocation -> {
              mdcDuringChain.set(MDC.get(CORRELATION_ID_MDC_KEY));
              return null;
            })
        .when(filterChain)
        .doFilter(any(), any());

    filter.doFilter(jsonRequest(), new MockHttpServletResponse(), filterChain);

    assertThat(mdcDuringChain.get()).isEqualTo(CORRELATION_ID);
    assertThat(MDC.get(CORRELATION_ID_MDC_KEY)).isNull();
    verify(filterChain).doFilter(any(), any());
  }

  @Test
  @DisplayName("Deve limpar o MDC e logar a entrada quando a requisição falha")
  void shouldClearMdcWhenChainThrows() throws Exception {
    doThrow(new ServletException()).when(filterChain).doFilter(any(), any());

    assertThatThrownBy(
            () -> filter.doFilter(jsonRequest(), new MockHttpServletResponse(), filterChain))
        .isInstanceOf(ServletException.class);

    assertThat(MDC.get(CORRELATION_ID_MDC_KEY)).isNull();
    assertThat(appender.list).hasSize(2);
  }

  @Test
  @DisplayName("Deve deixar o MDC sem correlation-id quando o header não é informado")
  void shouldLeaveMdcEmptyWhenHeaderIsAbsent() throws Exception {
    var request = new MockHttpServletRequest("POST", REQUEST_URI);
    var mdcDuringChain = new AtomicReference<String>();
    doAnswer(
            invocation -> {
              mdcDuringChain.set(MDC.get(CORRELATION_ID_MDC_KEY));
              return null;
            })
        .when(filterChain)
        .doFilter(any(), any());

    filter.doFilter(request, new MockHttpServletResponse(), filterChain);

    assertThat(mdcDuringChain.get()).isNull();
  }

  @Test
  @DisplayName("Deve passar reto sem logar quando o path está excluído")
  void shouldSkipLoggingWhenPathIsExcluded() throws Exception {
    var request = new MockHttpServletRequest("GET", EXCLUDED_URI);
    var response = new MockHttpServletResponse();

    filter.doFilter(request, response, filterChain);

    assertThat(appender.list).isEmpty();
    verify(filterChain).doFilter(request, response);
  }

  private MockHttpServletRequest jsonRequest() {
    var request = new MockHttpServletRequest("POST", REQUEST_URI);
    request.addHeader(CORRELATION_ID_HEADER, CORRELATION_ID);
    request.setContentType(APPLICATION_JSON_VALUE);
    request.setContent(REQUEST_BODY.getBytes(StandardCharsets.UTF_8));
    return request;
  }

  private void respondWithBody() throws Exception {
    doAnswer(
            invocation -> {
              entriesDuringChain.set(appender.list.size());
              var chainRequest = (HttpServletRequest) invocation.getArgument(0);
              chainRequest.getInputStream().readAllBytes();
              var chainResponse = (HttpServletResponse) invocation.getArgument(1);
              chainResponse.setContentType(APPLICATION_JSON_VALUE);
              chainResponse.getOutputStream().write(RESPONSE_BODY.getBytes(StandardCharsets.UTF_8));
              return null;
            })
        .when(filterChain)
        .doFilter(any(), any());
  }

  private String incomingMessage() {
    return appender.list.getFirst().getFormattedMessage();
  }

  private String outgoingMessage() {
    return appender.list.getLast().getFormattedMessage();
  }

  private Logger filterLogger() {
    return (Logger) LoggerFactory.getLogger(HttpLoggingFilter.class);
  }
}
