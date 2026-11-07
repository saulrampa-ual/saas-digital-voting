package com.saasvoting.identity.application.port.in;

import com.saasvoting.identity.domain.model.ApiKey;
import com.saasvoting.identity.domain.model.Caller;

import java.util.List;

/** RF-21 (D-47): the ADMIN of an organization manages the keys its integrations use. */
public interface ManageApiKeysUseCase {

    /** The key and its secret, which is shown only now. */
    record CreatedKey(ApiKey key, String clientSecret) {
    }

    List<ApiKey> list(Caller caller);

    /** {@code creatorName} is the name in the token of the caller, kept to show who created the key. */
    CreatedKey create(Caller caller, String name, String creatorName);

    ApiKey revoke(Caller caller, long id);
}
