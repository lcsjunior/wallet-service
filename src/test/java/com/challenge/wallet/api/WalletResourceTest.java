package com.challenge.wallet.api;

import com.challenge.wallet.dto.BalanceResponse;
import com.challenge.wallet.dto.CreateWalletResponse;
import com.challenge.wallet.dto.DepositRequest;
import com.challenge.wallet.dto.HistoricalBalanceResponse;
import com.challenge.wallet.dto.TransferRequest;
import com.challenge.wallet.dto.WithdrawRequest;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.Response;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.challenge.wallet.constants.Constants.NIL_UUID;
import static com.challenge.wallet.constants.Constants.ONE_CENT;
import static com.challenge.wallet.constants.TestConstants.INVALID_DECIMAL_AMOUNT;
import static com.challenge.wallet.constants.TestConstants.ONE_TRILLION;
import static com.challenge.wallet.constants.TestConstants.TEST_MAX_DATE;
import static com.challenge.wallet.constants.TestConstants.TEST_MIN_DATE;
import static com.challenge.wallet.constants.ValidationMessages.MSG_AMOUNT_INVALID_DECIMAL_SCALE;
import static com.challenge.wallet.constants.ValidationMessages.MSG_AMOUNT_MINIMUM_REQUIRED;
import static com.challenge.wallet.constants.ValidationMessages.MSG_CANNOT_TRANSFER_TO_SAME_WALLET;
import static com.challenge.wallet.constants.ValidationMessages.MSG_ENTITY_NOT_FOUND;
import static com.challenge.wallet.constants.ValidationMessages.MSG_INSUFFICIENT_BALANCE;
import static com.challenge.wallet.constants.ValidationMessages.MSG_INVALID_DATETIME_FORMAT;
import static com.challenge.wallet.factory.WalletTestFactory.toIsoString;
import static io.restassured.RestAssured.given;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
class WalletResourceTest {

    private CreateWalletResponse walletOne;
    private CreateWalletResponse walletTwo;

    @BeforeEach
    void setup() {
        walletOne = createWalletWithFunds(ONE_CENT)
                .as(CreateWalletResponse.class);
        walletTwo = createWalletWithFunds(ONE_CENT)
                .as(CreateWalletResponse.class);
    }

    @Test
    void shouldCreateWalletSuccessfully() {
        assertThat(walletOne.walletId())
                .isNotNull()
                .isInstanceOf(UUID.class);
    }

    @Nested
    class WhenRetrieveBalance {

        @Test
        void shouldRetrieveBalanceSuccessfully() {
            withdrawFunds(walletOne.walletId(), ONE_CENT);
            depositFunds(walletOne.walletId(), ONE_CENT);
            transferFunds(walletOne.walletId(), walletTwo.walletId(), ONE_CENT);
            depositFunds(walletOne.walletId(), ONE_TRILLION);
            BalanceResponse response = retrieveBalance(walletOne.walletId())
                    .then()
                    .statusCode(SC_OK)
                    .extract()
                    .as(BalanceResponse.class);
            assertThat(response.balance())
                    .isNotNull()
                    .usingComparator(BigDecimal::compareTo)
                    .isEqualTo(ONE_TRILLION);
            assertThat(response.updatedAt())
                    .isNotNull();
        }

        @Test
        void shouldThrowWalletNotFoundException() {
            retrieveBalance(NIL_UUID)
                    .then()
                    .statusCode(SC_NOT_FOUND)
                    .body("message", containsString(MSG_ENTITY_NOT_FOUND.getMessage()));
        }
    }

    @Nested
    class WhenRetrieveHistoricalBalance {

        @Test
        void shouldRetrieveHistoricalBalanceSuccessfully() {
            withdrawFunds(walletOne.walletId(), ONE_CENT);
            depositFunds(walletOne.walletId(), ONE_CENT.multiply(BigDecimal.valueOf(2)));
            transferFunds(walletOne.walletId(), walletTwo.walletId(), ONE_CENT);
            depositFunds(walletOne.walletId(), ONE_TRILLION);
            LocalDateTime currentDate = LocalDateTime.now();
            Awaitility.await()
                    .pollDelay(2, TimeUnit.SECONDS)
                    .timeout(3, TimeUnit.SECONDS)
                    .until(() -> true);
            depositFunds(walletOne.walletId(), ONE_CENT);
            HistoricalBalanceResponse response = given()
                    .when()
                    .pathParam("id", walletOne.walletId())
                    .queryParam("at", toIsoString(currentDate))
                    .get("/wallets/{id}/historical-balance")
                    .then()
                    .statusCode(SC_OK)
                    .extract()
                    .as(HistoricalBalanceResponse.class);
            assertThat(response.balance())
                    .isNotNull()
                    .usingComparator(BigDecimal::compareTo)
                    .isEqualTo(ONE_TRILLION.add(ONE_CENT));
            assertThat(response.at())
                    .isNotNull();
        }

        @Test
        void shouldReturnBalanceZeroIfNoHistory() {
            given()
                    .when()
                    .pathParam("id", walletOne.walletId())
                    .queryParam("at", toIsoString(TEST_MIN_DATE))
                    .get("/wallets/{id}/historical-balance")
                    .then()
                    .statusCode(SC_OK)
                    .body("balance", equalTo(0))
                    .body("at", notNullValue());
        }

        @Test
        void shouldThrowWalletNotFoundException() {
            given()
                    .when()
                    .pathParam("id", NIL_UUID)
                    .queryParam("at", toIsoString(TEST_MAX_DATE))
                    .get("/wallets/{id}/historical-balance")
                    .then()
                    .statusCode(SC_NOT_FOUND)
                    .body("message", containsString(MSG_ENTITY_NOT_FOUND.getMessage()));
        }

        @Test
        void shouldThrowDateTimeFormatException() {
            given()
                    .when()
                    .pathParam("id", walletOne.walletId())
                    .queryParam("at", "#")
                    .get("/wallets/{id}/historical-balance")
                    .then()
                    .statusCode(SC_BAD_REQUEST)
                    .body("message", containsString(MSG_INVALID_DATETIME_FORMAT.getMessage()));
        }
    }

    @Nested
    class WhenWithdrawing {

        @Test
        void shouldDecreaseBalance() {
            withdrawFunds(walletOne.walletId(), ONE_CENT)
                    .then()
                    .statusCode(SC_OK);
            BalanceResponse response = retrieveBalance(walletOne.walletId())
                    .then()
                    .statusCode(SC_OK)
                    .extract()
                    .as(BalanceResponse.class);
            assertThat(response.balance())
                    .isNotNull()
                    .usingComparator(BigDecimal::compareTo)
                    .isEqualTo(BigDecimal.ZERO);
        }

        @Test
        void shouldThrowWalletNotFoundException() {
            withdrawFunds(NIL_UUID, ONE_CENT)
                    .then()
                    .statusCode(SC_NOT_FOUND)
                    .body("message", containsString(MSG_ENTITY_NOT_FOUND.getMessage()));
        }

        @Test
        void shouldThrowAmountMinimumException() {
            withdrawFunds(walletOne.walletId(), BigDecimal.ZERO)
                    .then()
                    .statusCode(SC_BAD_REQUEST)
                    .body("message", containsString(MSG_AMOUNT_MINIMUM_REQUIRED.getMessage()));
        }

        @Test
        void shouldThrowInvalidAmountScaleException() {
            withdrawFunds(walletOne.walletId(), INVALID_DECIMAL_AMOUNT)
                    .then()
                    .statusCode(SC_BAD_REQUEST)
                    .body("message", containsString(MSG_AMOUNT_INVALID_DECIMAL_SCALE.getMessage()));
        }

        @Test
        void shouldThrowInsufficientBalanceException() {
            withdrawFunds(walletOne.walletId(), ONE_TRILLION)
                    .then()
                    .statusCode(SC_BAD_REQUEST)
                    .body("message", containsString(MSG_INSUFFICIENT_BALANCE.getMessage()));
            withdrawFunds(walletOne.walletId(), ONE_CENT);
            withdrawFunds(walletOne.walletId(), ONE_CENT)
                    .then()
                    .statusCode(SC_BAD_REQUEST)
                    .body("message", containsString(MSG_INSUFFICIENT_BALANCE.getMessage()));
        }
    }

    @Nested
    class WhenDepositing {

        @Test
        void shouldIncreaseBalance() {
            depositFunds(walletOne.walletId(), ONE_TRILLION)
                    .then()
                    .statusCode(SC_OK);
            BalanceResponse response = retrieveBalance(walletOne.walletId())
                    .then()
                    .statusCode(SC_OK)
                    .extract()
                    .as(BalanceResponse.class);
            assertThat(response.balance())
                    .isNotNull()
                    .usingComparator(BigDecimal::compareTo)
                    .isEqualTo(ONE_CENT.add(ONE_TRILLION));
        }

        @Test
        void shouldThrowWalletNotFoundException() {
            depositFunds(NIL_UUID, ONE_CENT)
                    .then()
                    .statusCode(SC_NOT_FOUND)
                    .body("message", containsString(MSG_ENTITY_NOT_FOUND.getMessage()));
        }

        @Test
        void shouldThrowAmountMinimumException() {
            depositFunds(walletOne.walletId(), BigDecimal.ZERO)
                    .then()
                    .statusCode(SC_BAD_REQUEST)
                    .body("message", containsString(MSG_AMOUNT_MINIMUM_REQUIRED.getMessage()));
        }

        @Test
        void shouldThrowInvalidAmountScaleException() {
            depositFunds(walletOne.walletId(), INVALID_DECIMAL_AMOUNT)
                    .then()
                    .statusCode(SC_BAD_REQUEST)
                    .body("message", containsString(MSG_AMOUNT_INVALID_DECIMAL_SCALE.getMessage()));
        }
    }

    @Nested
    class TransferringWallet {

        @Test
        void shouldDecreaseBalance() {
            withdrawFunds(walletTwo.walletId(), ONE_CENT);
            transferFunds(walletOne.walletId(), walletTwo.walletId(), ONE_CENT)
                    .then()
                    .statusCode(SC_OK);
            BalanceResponse response = retrieveBalance(walletOne.walletId())
                    .then()
                    .statusCode(SC_OK)
                    .extract()
                    .as(BalanceResponse.class);
            assertThat(response.balance())
                    .isNotNull()
                    .usingComparator(BigDecimal::compareTo)
                    .isEqualTo(BigDecimal.ZERO);
        }

        @Test
        void shouldIncreaseBalance() {
            transferFunds(walletOne.walletId(), walletTwo.walletId(), ONE_CENT)
                    .then()
                    .statusCode(SC_OK);
            BalanceResponse response = retrieveBalance(walletTwo.walletId())
                    .then()
                    .statusCode(SC_OK)
                    .extract()
                    .as(BalanceResponse.class);
            assertThat(response.balance())
                    .isNotNull()
                    .usingComparator(BigDecimal::compareTo)
                    .isEqualTo(ONE_CENT.add(ONE_CENT));
        }

        @Test
        void shouldThrowExceptionWhenTransferringToSameWallet() {
            transferFunds(walletOne.walletId(), walletOne.walletId(), ONE_CENT)
                    .then()
                    .statusCode(SC_BAD_REQUEST)
                    .body("message", containsString(MSG_CANNOT_TRANSFER_TO_SAME_WALLET.getMessage()));
        }

        @Test
        void shouldThrowWalletNotFoundException() {
            transferFunds(NIL_UUID, walletOne.walletId(), ONE_CENT)
                    .then()
                    .statusCode(SC_NOT_FOUND)
                    .body("message", containsString(MSG_ENTITY_NOT_FOUND.getMessage()));
            transferFunds(walletOne.walletId(), NIL_UUID, ONE_CENT)
                    .then()
                    .statusCode(SC_NOT_FOUND)
                    .body("message", containsString(MSG_ENTITY_NOT_FOUND.getMessage()));
        }

        @Test
        void shouldThrowAmountMinimumException() {
            transferFunds(walletOne.walletId(), walletTwo.walletId(), BigDecimal.ZERO)
                    .then()
                    .statusCode(SC_BAD_REQUEST)
                    .body("message", containsString(MSG_AMOUNT_MINIMUM_REQUIRED.getMessage()));
        }

        @Test
        void shouldThrowInvalidAmountScaleException() {
            transferFunds(walletOne.walletId(), walletTwo.walletId(), INVALID_DECIMAL_AMOUNT)
                    .then()
                    .statusCode(SC_BAD_REQUEST)
                    .body("message", containsString(MSG_AMOUNT_INVALID_DECIMAL_SCALE.getMessage()));
        }

        @Test
        void shouldThrowInsufficientBalanceException() {
            transferFunds(walletOne.walletId(), walletTwo.walletId(), ONE_TRILLION)
                    .then()
                    .statusCode(SC_BAD_REQUEST)
                    .body("message", containsString(MSG_INSUFFICIENT_BALANCE.getMessage()));
            transferFunds(walletOne.walletId(), walletTwo.walletId(), ONE_CENT);
            transferFunds(walletOne.walletId(), walletTwo.walletId(), ONE_CENT)
                    .then()
                    .statusCode(SC_BAD_REQUEST)
                    .body("message", containsString(MSG_INSUFFICIENT_BALANCE.getMessage()));
        }
    }

    private Response createWallet() {
        return given().when().post("/wallets");
    }

    private Response createWalletWithFunds(BigDecimal amount) {
        Response response = createWallet();
        CreateWalletResponse walletResponse = response
                .as(CreateWalletResponse.class);
        depositFunds(walletResponse.walletId(), amount);
        return response;
    }

    private Response withdrawFunds(UUID walletId, BigDecimal amount) {
        return given()
                .contentType(APPLICATION_JSON)
                .body(new WithdrawRequest(walletId, amount))
                .when()
                .post("/wallets/withdraw");
    }

    private Response depositFunds(UUID walletId, BigDecimal amount) {
        return given()
                .contentType(APPLICATION_JSON)
                .body(new DepositRequest(walletId, amount))
                .when()
                .post("/wallets/deposit");
    }

    private Response transferFunds(UUID fromWalletId, UUID toWalletId, BigDecimal amount) {
        return given()
                .contentType(APPLICATION_JSON)
                .body(new TransferRequest(fromWalletId, toWalletId, amount))
                .when()
                .post("/wallets/transfer");
    }

    private Response retrieveBalance(UUID walletId) {
        return given()
                .when()
                .pathParam("id", walletId)
                .get("/wallets/{id}/balance");
    }
}