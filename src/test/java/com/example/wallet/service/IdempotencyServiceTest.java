package com.example.wallet.service;

import static com.example.wallet.constants.Messages.CORRELATION_ID_CONFLICT;
import static com.example.wallet.entity.OperationType.DEPOSIT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.CONFLICT;

import com.example.wallet.entity.IdempotencyEntry;
import com.example.wallet.exception.ServiceException;
import com.example.wallet.repository.IdempotencyRepository;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IdempotencyServiceTest {

  private static final UUID CORRELATION_ID =
      UUID.fromString("4d8b7e15-2a90-4c63-8f21-7b0e5c9a3d16");

  private static final UUID WALLET_ID = UUID.fromString("2c9a1d4b-8e57-4f10-9b3a-5d61e0f27c48");

  @Mock private IdempotencyRepository idempotencyRepository;

  @InjectMocks private IdempotencyService idempotencyService;

  @Test
  @DisplayName("Deve seguir com a operação quando o Correlation-Id ainda não foi usado")
  void shouldNotReplayWhenCorrelationIdIsUnknown() {
    when(idempotencyRepository.findById(CORRELATION_ID)).thenReturn(Optional.empty());

    assertThat(idempotencyService.isReplay(entryWith("75.00"))).isFalse();

    verify(idempotencyRepository).findById(CORRELATION_ID);
  }

  @Test
  @DisplayName("Deve identificar replay quando o Correlation-Id repete os mesmos parâmetros")
  void shouldReplayWhenFingerprintMatches() {
    when(idempotencyRepository.findById(CORRELATION_ID))
        .thenReturn(Optional.of(entryWith("75.00")));

    assertThat(idempotencyService.isReplay(entryWith("75.00"))).isTrue();

    verify(idempotencyRepository).findById(CORRELATION_ID);
  }

  @Test
  @DisplayName("Deve rejeitar com 409 quando o Correlation-Id repete com parâmetros diferentes")
  void shouldRejectWhenFingerprintDiffers() {
    when(idempotencyRepository.findById(CORRELATION_ID))
        .thenReturn(Optional.of(entryWith("10.00")));

    assertThatThrownBy(() -> idempotencyService.isReplay(entryWith("75.00")))
        .isInstanceOfSatisfying(
            ServiceException.class,
            exception -> assertThat(exception.getHttpStatus()).isEqualTo(CONFLICT))
        .hasMessage(CORRELATION_ID_CONFLICT);

    verify(idempotencyRepository).findById(CORRELATION_ID);
  }

  @Test
  @DisplayName("Deve persistir a entrada quando a operação é concluída")
  void shouldPersistEntryWhenSaving() {
    var idempotencyEntry = entryWith("75.00");

    idempotencyService.save(idempotencyEntry);

    verify(idempotencyRepository).save(idempotencyEntry);
  }

  private static IdempotencyEntry entryWith(String amount) {
    return IdempotencyEntry.builder()
        .correlationId(CORRELATION_ID)
        .operationType(DEPOSIT)
        .key(WALLET_ID.toString())
        .amount(new BigDecimal(amount))
        .build();
  }
}
