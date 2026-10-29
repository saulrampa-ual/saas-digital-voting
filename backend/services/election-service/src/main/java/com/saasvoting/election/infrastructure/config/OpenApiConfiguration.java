package com.saasvoting.election.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.Scopes;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Generated OpenAPI document with Keycloak login for Swagger UI (D-26). */
@Configuration
public class OpenApiConfiguration {

    private static final String KEYCLOAK_SCHEME = "keycloak";

    @Bean
    OpenAPI electionOpenApi(@Value("${verivoto.swagger.keycloak-realm-url}") String realmUrl) {
        OAuthFlow authorizationCode = new OAuthFlow()
                .authorizationUrl(realmUrl + "/protocol/openid-connect/auth")
                .tokenUrl(realmUrl + "/protocol/openid-connect/token")
                .scopes(new Scopes().addString("openid", "OpenID Connect"));
        return new OpenAPI()
                .info(new Info()
                        .title("VeriVoto election-service API")
                        .version("v1")
                        .description("Census, voting processes, ballots and lifecycle for the VeriVoto platform."))
                .components(new Components().addSecuritySchemes(KEYCLOAK_SCHEME, new SecurityScheme()
                        .type(SecurityScheme.Type.OAUTH2)
                        .flows(new OAuthFlows().authorizationCode(authorizationCode))))
                .addSecurityItem(new SecurityRequirement().addList(KEYCLOAK_SCHEME));
    }
}
