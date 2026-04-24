package com.yellowleaf.coulson.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${server.port:8080}")
    private int serverPort;

    @Value("${swagger.external-url:}")
    private String externalUrl;

    @Bean
    public OpenAPI openAPI() {
        List<Server> servers = new ArrayList<>();
        if (externalUrl != null && !externalUrl.isBlank()) {
            servers.add(new Server().url(externalUrl).description("Production (via Kong)"));
        }
        servers.add(new Server().url("http://localhost:" + serverPort).description("Direct (local)"));

        return new OpenAPI()
                .info(new Info()
                        .title("Coulson Assistant API")
                        .description("""
                                Service for video transcription via AI webhook.
                                
                                Receives a video file via multipart upload, automatically splits it into chunks \
                                of up to 200 MB when necessary, sends each chunk to the configured transcription \
                                webhook, and returns the combined transcription as a single text.
                                """)
                        .version("1.0.1")
                        .contact(new Contact()
                                .name("Yellowleaf")
                                .email("contato@yellowleaf.com.br")))
                .servers(servers)
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .name("bearerAuth")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Supabase JWT token. Obtain via Supabase Auth or use ANON_KEY / SERVICE_ROLE_KEY.")));
    }
}
