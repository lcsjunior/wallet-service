package com.example.wallet.exception;

import org.springframework.http.HttpStatus;

public class ServiceException extends RuntimeException {

  private final HttpStatus httpStatus;

  public ServiceException(String message, HttpStatus httpStatus) {
    super(message);
    this.httpStatus = httpStatus;
  }

  public ServiceException(ErrorCode errorCode) {
    this(errorCode.getMessageKey(), errorCode.getHttpStatus());
  }

  public HttpStatus getHttpStatus() {
    return httpStatus;
  }
}
