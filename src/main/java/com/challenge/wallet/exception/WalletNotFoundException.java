package com.challenge.wallet.exception;

import static jakarta.ws.rs.core.Response.Status.NOT_FOUND;

public class WalletNotFoundException extends ServiceException {

    public static final String MESSAGE = "Wallet not found";

    public WalletNotFoundException() {
        super(MESSAGE, NOT_FOUND);
    }
}
