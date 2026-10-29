package com.saasvoting.voteraccess.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {

    @Bean
    OpenAPI voteraccessOpenApi() {
        return new OpenAPI().info(new Info()
                .title("VeriVoto voter-access-service API")
                .version("v1")
                .description("Voter access, one-time codes, voting tokens, participation and notifications for the VeriVoto platform."));
    }
}
