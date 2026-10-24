package com.edifika.telemetry;

import com.edifika.telemetry.support.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class ApplicationContextIT {

    @LocalServerPort
    private int port;

    private final TestRestTemplate http = new TestRestTemplate();

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    @Test
    void protectedEndpointsRequireToken() {
        assertEquals(401, http.getForEntity(url("/api/v1/telemetry/devices"), String.class).getStatusCode().value());
    }

    @Test
    void swaggerIsPublic() {
        assertEquals(200, http.getForEntity(url("/v3/api-docs"), String.class).getStatusCode().value());
    }
}
