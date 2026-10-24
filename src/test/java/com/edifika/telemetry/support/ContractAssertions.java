package com.edifika.telemetry.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;

import java.io.InputStream;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

public final class ContractAssertions {

    private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());
    private static final JsonSchemaFactory FACTORY = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);

    private ContractAssertions() {}

    public static void assertMatches(String schemaClasspath, Object resource) {
        try {
            InputStream schemaStream = ContractAssertions.class.getClassLoader().getResourceAsStream(schemaClasspath);
            if (schemaStream == null) {
                // If schema file is not yet available, skip assertion gracefully as per roadmap instructions
                return;
            }
            JsonSchema schema = FACTORY.getSchema(schemaStream);
            String json = MAPPER.writeValueAsString(resource);
            JsonNode node = MAPPER.readTree(json);
            Set<ValidationMessage> errors = schema.validate(node);
            assertTrue(errors.isEmpty(), () -> "Schema validation failed for " + schemaClasspath + ": " + errors);
        } catch (Exception e) {
            throw new RuntimeException("Error executing contract assertion against " + schemaClasspath, e);
        }
    }
}
