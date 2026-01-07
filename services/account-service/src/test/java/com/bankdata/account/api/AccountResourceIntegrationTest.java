package com.bankdata.account.api;

import com.bankdata.account.api.dto.CreateAccountRequest;
import com.bankdata.account.api.dto.DepositRequest;
import com.bankdata.account.api.dto.TransferRequest;
import com.bankdata.account.messaging.AccountEventPublisher;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@QuarkusTest
class AccountResourceIntegrationTest {

    @InjectMock
    AccountEventPublisher eventPublisher;

    @Test
    void health_isOk() {
        given()
                .when().get("/health")
                .then()
                .statusCode(200)
                .body(equalTo("OK"));
    }

    @Test
    void createAccount_returns201_andPayload() {
        CreateAccountRequest request = new CreateAccountRequest("Denis", "Ignatenko", new BigDecimal("10.00"));

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/accounts")
                .then()
                .statusCode(201)
                .contentType(ContentType.JSON)
                .body("accountNumber", not(emptyOrNullString()))
                .body("balance", equalTo(10.00f));

        verify(eventPublisher, atLeastOnce()).safePublish(any());
    }

    @Test
    void deposit_updatesBalance() {
        String acc = createAccountAndGetNumber(new BigDecimal("0.00"));

        DepositRequest depositRequest = new DepositRequest(new BigDecimal("5.00"));

        given()
                .contentType(ContentType.JSON)
                .body(depositRequest)
                .when()
                .post("/accounts/{acc}/deposit", acc)
                .then()
                .statusCode(200)
                .body("accountNumber", equalTo(acc))
                .body("balance", equalTo(5.00f));

        verify(eventPublisher, atLeastOnce()).safePublish(any());
    }

    @Test
    void transfer_movesMoneyBetweenAccounts() {
        String from = createAccountAndGetNumber(new BigDecimal("100.00"));
        String to = createAccountAndGetNumber(new BigDecimal("0.00"));

        TransferRequest request = new TransferRequest(from, to, new BigDecimal("10.00"));

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/accounts/transfer")
                .then()
                .statusCode(200)
                .body("fromAccountNumber", equalTo(from))
                .body("fromBalance", equalTo(90.00f))
                .body("toAccountNumber", equalTo(to))
                .body("toBalance", equalTo(10.00f));

        verify(eventPublisher, atLeastOnce()).safePublish(any());
    }

    @Test
    void balance_returnsCurrentBalance() {
        String account = createAccountAndGetNumber(new BigDecimal("12.34"));

        given()
                .when()
                .get("/accounts/{acc}/balance", account)
                .then()
                .statusCode(200)
                .body("accountNumber", equalTo(account))
                .body("balance", equalTo(12.34f));
    }

    @Test
    void transfer_sameAccount_returns400() {
        String account = createAccountAndGetNumber(new BigDecimal("10.00"));

        TransferRequest request = new TransferRequest(account, account, new BigDecimal("1.00"));

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/accounts/transfer")
                .then()
                .statusCode(400);
    }

    @Test
    void deposit_invalidAmount_returns400() {
        String account = createAccountAndGetNumber(new BigDecimal("0.00"));

        given()
                .contentType(ContentType.JSON)
                .body("{\"amount\": null}")
                .when()
                .post("/accounts/{acc}/deposit", account)
                .then()
                .statusCode(400);
    }


    private String createAccountAndGetNumber(BigDecimal initialDeposit) {
        CreateAccountRequest request = new CreateAccountRequest("Test", "User", initialDeposit);

        return given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/accounts")
                .then()
                .statusCode(201)
                .extract()
                .path("accountNumber");
    }
}