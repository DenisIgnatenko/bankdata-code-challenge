package com.bankdata.analytics.api;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

@QuarkusTest
class AnalyticsResourceIntegrationTest {

    @Test
    void events_returns200_andArray() {
        given()
                .when()
                .get("/analytics/events?limit=10")
                .then()
                .statusCode(200)
                .body("$", is(org.hamcrest.Matchers.instanceOf(java.util.List.class)));
    }
}