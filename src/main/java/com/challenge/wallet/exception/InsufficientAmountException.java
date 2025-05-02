package com.challenge.wallet.exception;

import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;

public class InsufficientAmountException extends ServiceException {

    public static final String MESSAGE = "Amount must be greater than or equal to 0.01";

    public InsufficientAmountException() {
        super(MESSAGE, BAD_REQUEST);
    }
}
