package com.saasvoting.identity.infrastructure.adapter.in.rest;

import com.saasvoting.identity.application.port.in.ManageApiKeysUseCase;
import com.saasvoting.identity.domain.model.ApiKey;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.CacheControl;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * RF-21 (D-47): keys of the organization of the ADMIN in the token (checked by the use case). The secret appears
 * only in the answer to the creation and is never stored by VeriVoto.
 */
@RestController
@RequestMapping("/api/v1/api-keys")
public class ApiKeyController {

    public record CreateApiKeyRequest(@NotBlank @Size(max = 60) String name) {
    }

    public record ApiKeyResponse(long id, String name, String clientId, String createdByName, Instant createdAt,
            Instant revokedAt, boolean active) {

        static ApiKeyResponse of(ApiKey key) {
            return new ApiKeyResponse(key.id(), key.name(), key.clientId(), key.createdByName(), key.createdAt(),
                    key.revokedAt(), key.isActive());
        }
    }

    public record CreatedApiKeyResponse(ApiKeyResponse key, String clientSecret, String tokenUrl) {
    }

    public record ApiKeysResponse(List<ApiKeyResponse> items, int maxActive) {
    }

    private final ManageApiKeysUseCase apiKeys;
    private final AdminActionRecorder actions;
    private final String tokenUrl;

    public ApiKeyController(ManageApiKeysUseCase apiKeys, AdminActionRecorder actions,
            @org.springframework.beans.factory.annotation.Value(
                    "${verivoto.api-keys.token-url:http://localhost:8080/realms/verivoto/protocol/openid-connect/token}")
            String tokenUrl) {
        this.apiKeys = Objects.requireNonNull(apiKeys, "apiKeys must not be null");
        this.actions = Objects.requireNonNull(actions, "actions must not be null");
        this.tokenUrl = Objects.requireNonNull(tokenUrl, "tokenUrl must not be null");
    }

    @GetMapping
    public ApiKeysResponse list(@AuthenticationPrincipal Jwt jwt) {
        return new ApiKeysResponse(apiKeys.list(CallerResolver.from(jwt)).stream().map(ApiKeyResponse::of).toList(),
                ApiKey.MAX_ACTIVE);
    }

    @PostMapping
    public ResponseEntity<CreatedApiKeyResponse> create(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateApiKeyRequest request) {
        ManageApiKeysUseCase.CreatedKey created = apiKeys.create(CallerResolver.from(jwt), request.name(),
                AdminActionRecorder.actorName(jwt));
        ApiKey key = created.key();
        actions.record(jwt, key.organizationId(), "API_KEY_CREATED", "API_KEY", key.id().toString(), key.name(),
                key.clientId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.LOCATION, "/api/v1/api-keys/" + key.id())
                .cacheControl(CacheControl.noStore())
                .body(new CreatedApiKeyResponse(ApiKeyResponse.of(key), created.clientSecret(), tokenUrl));
    }

    @DeleteMapping("/{id}")
    public ApiKeyResponse revoke(@AuthenticationPrincipal Jwt jwt, @PathVariable long id) {
        ApiKey key = apiKeys.revoke(CallerResolver.from(jwt), id);
        actions.record(jwt, key.organizationId(), "API_KEY_REVOKED", "API_KEY", key.id().toString(), key.name(),
                key.clientId());
        return ApiKeyResponse.of(key);
    }
}
