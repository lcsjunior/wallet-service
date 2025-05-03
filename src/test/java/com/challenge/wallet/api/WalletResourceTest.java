package com.challenge.wallet.api;

import com.challenge.wallet.dto.CreateWalletResponse;
import com.challenge.wallet.dto.DepositRequest;
import com.challenge.wallet.dto.TransferRequest;
import com.challenge.wallet.dto.WithdrawRequest;
import com.challenge.wallet.service.WalletService;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static com.challenge.wallet.constants.Constants.ONE_CENT;
import static com.challenge.wallet.constants.TestConstants.TEST_MIN_DATE;
import static com.challenge.wallet.constants.TestConstants.TEST_UUID_1;
import static com.challenge.wallet.constants.TestConstants.TEST_UUID_2;
import static com.challenge.wallet.constants.TestConstants.TEST_UUID_3;
import static com.challenge.wallet.factory.WalletTestFactory.toIsoString;
import static io.restassured.RestAssured.given;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.apache.http.HttpStatus.SC_CREATED;

import static org.apache.http.HttpStatus.SC_OK;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.Is.is;

@QuarkusTest
class WalletResourceTest {

    @Inject
    WalletService walletService;

    @Test
    void shouldCreateWalletSuccessfully() {
        CreateWalletResponse response =
                given()
                        .when().post("/wallet")
                        .then()
                        .statusCode(SC_CREATED)
                        .extract()
                        .as(CreateWalletResponse.class);
        assertThat(response.walletId())
                .isNotNull()
                .isInstanceOf(UUID.class);
    }

    @Test
    void shouldReturnBalanceForWallet() {
        given()
                .when()
                .pathParam("id", TEST_UUID_1)
                .get("/wallet/{id}/balance")
                .then()
                .statusCode(SC_OK)
                .body("balance", is(ONE_CENT.floatValue()))
                .body("updatedAt", notNullValue());
    }

    @Test
    void shouldReturnHistoricalBalanceForWallet() {
        given()
                .when()
                .pathParam("id", TEST_UUID_1)
                .queryParam("at", toIsoString(TEST_MIN_DATE))
                .get("/wallet/{id}/historical-balance")
                .then()
                .statusCode(SC_OK)
                .body("balance", is(BigDecimal.ZERO.floatValue()))
                .body("at", is(toIsoString(TEST_MIN_DATE)));
    }

    @Test
    void shouldIncreaseBalanceOnDeposit() {
        given()
                .contentType(APPLICATION_JSON)
                .body(new DepositRequest(TEST_UUID_2, ONE_CENT))
                .when()
                .post("/wallet/deposit")
                .then()
                .statusCode(SC_OK);
    }

    @Test
    void shouldDecreaseBalanceOnWithdraw() {
        given()
                .contentType(APPLICATION_JSON)
                .body(new WithdrawRequest(TEST_UUID_2, ONE_CENT))
                .when()
                .post("/wallet/withdraw")
                .then()
                .statusCode(SC_OK);
    }

    @Test
    void shouldIncreaseBalanceOnTransfer() {
        given()
                .contentType(APPLICATION_JSON)
                .body(new TransferRequest(TEST_UUID_2, TEST_UUID_3, ONE_CENT))
                .when()
                .post("/wallet/transfer")
                .then()
                .statusCode(SC_OK);
    }
}