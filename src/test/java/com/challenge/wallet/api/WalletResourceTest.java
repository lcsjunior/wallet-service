package com.challenge.wallet.api;

import com.challenge.wallet.dto.BalanceResponse;
import com.challenge.wallet.dto.CreateWalletResponse;
import com.challenge.wallet.exception.WalletNotFoundException;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static com.challenge.wallet.constants.TestConstants.NIL_UUID;
import static com.challenge.wallet.constants.TestConstants.TEST_UUID_1;
import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_CREATED;

import static org.apache.http.HttpStatus.SC_OK;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
class WalletResourceTest {

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
                .body("balance", notNullValue())
                .body("updatedAt", notNullValue());
    }

    @Test
    void shouldReturnBadRequestForNilUUID() {
        given()
                .when()
                .pathParam("id", NIL_UUID)
                .get("/wallet/{id}/balance")
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", containsString(WalletNotFoundException.MESSAGE));
    }
}