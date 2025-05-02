package com.challenge.wallet.exception;

import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;

public class DecimalScaleException extends ServiceException {

    public static final String MESSAGE = "Amount must have at most two decimal places";

    public DecimalScaleException() {
        super(MESSAGE, BAD_REQUEST);
    }
}
