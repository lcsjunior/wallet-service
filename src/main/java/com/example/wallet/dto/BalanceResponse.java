package com.example.wallet.dto;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;

public record BalanceResponse(@JsonFormat(shape = STRING) BigDecimal balance) {}
