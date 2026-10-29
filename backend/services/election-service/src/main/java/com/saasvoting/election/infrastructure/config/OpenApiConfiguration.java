package com.saasvoting.election.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {

    @Bean
    OpenAPI electionOpenApi() {
        return new OpenAPI().info(new Info()
                .title("VeriVoto election-service API")
                .version("v1")
                .description("Census, voting processes, ballots and lifecycle for the VeriVoto platform."));
    }
}
