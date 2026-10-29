package com.saasvoting.ballotbox.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {

    @Bean
    OpenAPI ballotboxOpenApi() {
        return new OpenAPI().info(new Info()
                .title("VeriVoto ballot-box-service API")
                .version("v1")
                .description("Ballot box, receipts, hash-chained audit log, tally and minutes for the VeriVoto platform."));
    }
}
