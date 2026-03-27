package com.example.queryapi;

import com.example.queryapi.model.DemoStats;
import com.example.queryapi.support.ExpectedOutput;
import com.example.queryapi.support.Fixture;
import com.example.queryapi.support.FixtureLoader;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
public class FixtureTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    static Stream<Arguments> loadFixtures() throws Exception {
        return FixtureLoader.loadFixtures();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("loadFixtures")
    void fixtureTest(String name, Fixture fixture) throws Exception {
        String requestJson = MAPPER.writeValueAsString(fixture.request);

        ValidatableResponse response = given()
            .contentType(ContentType.JSON)
            .body(requestJson)
        .when()
            .post("/api/query")
        .then()
            .statusCode(200);

        ExpectedOutput exp = fixture.expected;

        if (exp.columns != null) {
            response.body("columns", equalTo(exp.columns));
        }

        if (exp.count != null) {
            response.body("count", equalTo(exp.count));
        }

        if (exp.demoStats != null) {
            DemoStats c = exp.demoStats;
            response.body("demoStats.total", equalTo(c.total));
            response.body("demoStats.m", equalTo(c.m));
            response.body("demoStats.f", equalTo(c.f));
            response.body("demoStats.w", equalTo(c.w));
            response.body("demoStats.b", equalTo(c.b));
            response.body("demoStats.i", equalTo(c.i));
            response.body("demoStats.a", equalTo(c.a));
            response.body("demoStats.h", equalTo(c.h));
            response.body("demoStats.o", equalTo(c.o));
        }

        if (exp.rows != null) {
            response.body("rows", hasSize(exp.rows.size()));
            for (int i = 0; i < exp.rows.size(); i++) {
                for (var entry : exp.rows.get(i).entrySet()) {
                    String path = "rows[" + i + "]." + entry.getKey();
                    response.body(path, equalTo(entry.getValue()));
                }
            }
        }
    }
}
