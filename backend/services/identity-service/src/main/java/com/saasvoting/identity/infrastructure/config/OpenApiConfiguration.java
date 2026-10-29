package com.saasvoting.identity.infrastructure.config;

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

/**
 * Generated OpenAPI document. Swagger UI logs in against Keycloak with Authorization Code + PKCE using the
 * development-only client {@code verivoto-dev-swagger} (D-26).
 */
@Configuration
public class OpenApiConfiguration {

    private static final String KEYCLOAK_SCHEME = "keycloak";

    @Bean
    OpenAPI identityOpenApi(@Value("${verivoto.swagger.keycloak-realm-url}") String realmUrl) {
        OAuthFlow authorizationCode = new OAuthFlow()
                .authorizationUrl(realmUrl + "/protocol/openid-connect/auth")
                .tokenUrl(realmUrl + "/protocol/openid-connect/token")
                .scopes(new Scopes().addString("openid", "OpenID Connect"));
        return new OpenAPI()
                .info(new Info()
                        .title("VeriVoto identity-service API")
                        .version("v1")
                        .description("Organizations, roles, privacy and administrative traceability for the VeriVoto platform."))
                .components(new Components().addSecuritySchemes(KEYCLOAK_SCHEME, new SecurityScheme()
                        .type(SecurityScheme.Type.OAUTH2)
                        .flows(new OAuthFlows().authorizationCode(authorizationCode))))
                .addSecurityItem(new SecurityRequirement().addList(KEYCLOAK_SCHEME));
    }
}
