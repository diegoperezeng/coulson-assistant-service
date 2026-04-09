package com.yellowleaf.coulson.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${server.port:8080}")
    private int serverPort;

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Coulson Assistant API")
                        .description("""
                                Service for video transcription via AI webhook.
                                
                                Receives a video file via multipart upload, automatically splits it into chunks \
                                of up to 200 MB when necessary, sends each chunk to the configured transcription \
                                webhook, and returns the combined transcription as a single text.
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Yellowleaf")
                                .email("contato@yellowleaf.com.br")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("Local")));
    }
}
