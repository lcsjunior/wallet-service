package com.challenge.wallet.exception;

import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;

public class DatetimeFormatException extends ServiceException {

    public static final String MESSAGE = "Invalid datetime format. Use ISO format like '2025-05-01T15:30:00'";

    public DatetimeFormatException() {
        super(MESSAGE, BAD_REQUEST);
    }
}
