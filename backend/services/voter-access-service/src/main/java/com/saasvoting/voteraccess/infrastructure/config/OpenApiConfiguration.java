package com.saasvoting.voteraccess.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Generated OpenAPI document. Voter endpoints need no login; internal endpoints need a SERVICE token, which Swagger
 * UI cannot obtain (client credentials), so they are tested as described in {@code docs/architecture/api-testing.md}.
 */
@Configuration
public class OpenApiConfiguration {

    @Bean
    OpenAPI voteraccessOpenApi() {
        return new OpenAPI().info(new Info()
                .title("VeriVoto voter-access-service API")
                .version("v1")
                .description("Voter access with personal link and email code, voting tokens, participation and "
                        + "invitations for the VeriVoto platform."));
    }
}
