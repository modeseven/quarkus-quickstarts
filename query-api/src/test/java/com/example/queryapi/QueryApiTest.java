package com.example.queryapi;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
public class QueryApiTest {

    @Test
    void smokeTest_arstRoster_returnsRows() {
        String body = """
            {
              "function": "ROSTER",
              "selectionCategory": "ARST",
              "columns": ["REG", "LN", "FN", "FACL"],
              "orgScope": { "type": "FACL", "code": "BOP" }
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(body)
        .when()
            .post("/api/query")
        .then()
            .statusCode(200)
            .body("count", greaterThan(0))
            .body("columns", hasSize(4))
            .body("columns", contains("REG", "LN", "FN", "FACL"))
            .body("demoStats.total", greaterThan(0))
            .body("rows", not(empty()));
    }
}
