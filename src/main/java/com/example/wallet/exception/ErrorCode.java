package com.example.wallet.exception;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
  WALLET_NOT_FOUND(NOT_FOUND, "wallet.notfound"),
  INSUFFICIENT_BALANCE(UNPROCESSABLE_ENTITY, "wallet.insufficientbalance"),
  CORRELATION_ID_CONFLICT(CONFLICT, "correlationid.conflict"),
  SAME_WALLET_TRANSFER(BAD_REQUEST, "transfer.samewallet"),
  MISSING_REQUIRED_HEADER(BAD_REQUEST, "header.missing"),
  VALIDATION_ERROR(BAD_REQUEST, "validation.error");

  private final HttpStatus httpStatus;
  private final String messageKey;

  ErrorCode(HttpStatus httpStatus, String messageKey) {
    this.httpStatus = httpStatus;
    this.messageKey = messageKey;
  }

  public HttpStatus getHttpStatus() {
    return httpStatus;
  }

  public String getMessageKey() {
    return messageKey;
  }
}
