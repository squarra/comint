package org.example;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static io.restassured.RestAssured.given;

@Tag("requires-rabbitmq")
@QuarkusTest
class UICMessageEndpointTest {

    private static final String ENDPOINT = "/LIMessageProcessing/http/UICCCMessageProcessing/UICCCMessageProcessingInboundWS";

    @Test
    void shouldValidateRequest() throws IOException {
        Path resourcePath = Paths.get("src/test/resources/ReceiptConfirmationMessage.xml");
        String soapRequest = Files.readString(resourcePath);

        given()
                .contentType(ContentType.XML)
                .body(soapRequest)
                .when()
                .post(ENDPOINT)
                .then()
                .statusCode(200);
    }
}
