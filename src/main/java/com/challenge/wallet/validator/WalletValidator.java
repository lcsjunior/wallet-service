package com.challenge.wallet.validator;

import com.challenge.wallet.constants.Constants;
import com.challenge.wallet.exception.ServiceException;

import java.math.BigDecimal;
import java.util.UUID;

import static com.challenge.wallet.constants.Constants.CURRENCY_DECIMAL_SCALE;
import static com.challenge.wallet.constants.ValidationMessages.MSG_AMOUNT_INVALID_DECIMAL_SCALE;
import static com.challenge.wallet.constants.ValidationMessages.MSG_AMOUNT_MINIMUM_REQUIRED;
import static com.challenge.wallet.constants.ValidationMessages.MSG_CANNOT_TRANSFER_TO_SAME_WALLET;
import static com.challenge.wallet.constants.ValidationMessages.MSG_INSUFFICIENT_BALANCE;

public class WalletValidator {

    // NOSONAR
    private WalletValidator() {
        throw new IllegalStateException();
    }

    public static void validateAvailableAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(Constants.ONE_CENT) < 0) {
            throw new ServiceException(MSG_AMOUNT_MINIMUM_REQUIRED.getMessage());
        }
        if (amount.scale() > CURRENCY_DECIMAL_SCALE) {
            throw new ServiceException(MSG_AMOUNT_INVALID_DECIMAL_SCALE.getMessage());
        }
    }

    public static void validateAvailableBalance(BigDecimal balance, BigDecimal amount) {
        if (balance == null || balance.compareTo(amount) < 0) {
            throw new ServiceException(MSG_INSUFFICIENT_BALANCE.getMessage());
        }
    }

    public static void validateSameWallet(UUID walletIdOne, UUID walletIdTwo) {
        if (walletIdOne.equals(walletIdTwo)) {
            throw new ServiceException(MSG_CANNOT_TRANSFER_TO_SAME_WALLET.getMessage());
        }
    }
}
