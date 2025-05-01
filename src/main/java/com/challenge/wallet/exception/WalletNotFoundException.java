package com.challenge.wallet.exception;

import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;

public class WalletNotFoundException extends ServiceException {

    public static String MESSAGE = "Wallet not found";

    public WalletNotFoundException() {
        super(MESSAGE, BAD_REQUEST);
    }
}
