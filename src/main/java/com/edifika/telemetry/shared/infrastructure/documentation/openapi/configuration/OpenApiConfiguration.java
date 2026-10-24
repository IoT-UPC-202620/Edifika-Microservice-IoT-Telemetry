package com.edifika.telemetry.shared.infrastructure.documentation.openapi.configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfiguration {

    @Value("${server.port:8090}")
    private String serverPort;

    @Bean
    public OpenAPI edifikaPlatformOpenApi() {
        var openApi = new OpenAPI();

        openApi.info(new Info()
                        .title("Edifika API - IoT Telemetry Microservice")
                        .description("Backend API for Edifika IoT Telemetry Microservice")
                        .version("0.1.0")
                        .contact(new Contact()
                                .name("Condomia Development Team")
                                .email("dev@edifika.com")
                                .url("https://edifika.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .externalDocs(new ExternalDocumentation()
                        .description("Edifika Platform Documentation")
                        .url("https://docs.edifika.com"))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("Servidor local de desarrollo")
                ));

        final String securitySchemeName = "bearerAuth";

        openApi.addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));

        return openApi;
    }
}
