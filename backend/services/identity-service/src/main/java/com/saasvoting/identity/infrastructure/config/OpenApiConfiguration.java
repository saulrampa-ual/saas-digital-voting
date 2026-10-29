package com.saasvoting.identity.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {

    @Bean
    OpenAPI identityOpenApi() {
        return new OpenAPI().info(new Info()
                .title("VeriVoto identity-service API")
                .version("v1")
                .description("Organizations, roles, privacy and administrative traceability for the VeriVoto platform."));
    }
}
