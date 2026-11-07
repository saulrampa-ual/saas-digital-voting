package com.saasvoting.ballotbox.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Generated OpenAPI document. Voter endpoints need no login; internal ones need a SERVICE token. */
@Configuration
public class OpenApiConfiguration {

    @Bean
    OpenAPI ballotBoxOpenApi() {
        return new OpenAPI().info(new Info()
                .title("VeriVoto ballot-box-service API")
                .version("v1")
                .description("Ballot, vote casting with receipt, receipt verification and hash-chained audit log "
                        + "for the VeriVoto platform. No identity, token or time of vote is stored."));
    }
}
