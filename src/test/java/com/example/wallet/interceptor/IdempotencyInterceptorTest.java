package com.example.wallet.interceptor;

import static com.example.wallet.constants.AppHeader.IDEMPOTENCY_KEY_HEADER;
import static com.example.wallet.constants.Messages.ENTITY_CONFLICT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.CONFLICT;

import com.example.wallet.exception.ServiceException;
import com.example.wallet.service.IdempotencyService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@ExtendWith(MockitoExtension.class)
class IdempotencyInterceptorTest {

  private static final String REQUEST_URI = "/v1/transfers";

  private static final String IDEMPOTENCY_KEY = "3d7f5b62-91ae-4c08-8e23-7b4a0c6d5f19";

  private final MockHttpServletRequest request = new MockHttpServletRequest("POST", REQUEST_URI);

  private final MockHttpServletResponse response = new MockHttpServletResponse();

  @Mock private IdempotencyService idempotencyService;

  @InjectMocks private IdempotencyInterceptor idempotencyInterceptor;

  @Test
  @DisplayName("Deve seguir sem reservar quando o header de idempotência está ausente")
  void shouldSkipReservationWhenHeaderIsMissing() {
    assertThat(idempotencyInterceptor.preHandle(request, response, new Object())).isTrue();

    verify(idempotencyService, never()).reserve(anyString(), anyString());
  }

  @Test
  @DisplayName("Deve seguir quando a chave é reservada com sucesso")
  void shouldProceedWhenKeyIsReserved() {
    request.addHeader(IDEMPOTENCY_KEY_HEADER, IDEMPOTENCY_KEY);
    when(idempotencyService.reserve(REQUEST_URI, IDEMPOTENCY_KEY)).thenReturn(true);

    assertThat(idempotencyInterceptor.preHandle(request, response, new Object())).isTrue();
  }

  @Test
  @DisplayName("Deve lançar conflito quando a chave já está reservada")
  void shouldRejectWhenKeyIsAlreadyReserved() {
    request.addHeader(IDEMPOTENCY_KEY_HEADER, IDEMPOTENCY_KEY);
    when(idempotencyService.reserve(REQUEST_URI, IDEMPOTENCY_KEY)).thenReturn(false);

    assertThatExceptionOfType(ServiceException.class)
        .isThrownBy(() -> idempotencyInterceptor.preHandle(request, response, new Object()))
        .withMessage(ENTITY_CONFLICT)
        .satisfies(ex -> assertThat(ex.getHttpStatus()).isEqualTo(CONFLICT));
  }
}
