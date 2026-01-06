package com.bankdata.account.api;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
class HealthResourceTest {

    @Test
    void health_returns_ok() {
        given()
                .when().get("/health")
                .then()
                .statusCode(200)
                .body(is("OK"));
    }
}