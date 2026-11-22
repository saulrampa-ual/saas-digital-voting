# IMP-48: pinned by digest (OCI index, all platforms). Update the digest together with the tag:
#   docker buildx imagetools inspect quay.io/keycloak/keycloak:<version>
FROM quay.io/keycloak/keycloak:26.8.1@sha256:3f3c5f8e03c614774b4bd9173b88391fefa4153bfc010d8012d59ad825377332

USER root

ADD --checksum=sha256:8013758fe4f24dfb561d57f83c0853387fb18c9b35925c77fb60ccd9af7cedea \
    https://github.com/for-keycloak/email-otp-authenticator/releases/download/v1.8.1/email-otp-authenticator-v1.8.1-kc-26.8.1.jar \
    /opt/keycloak/providers/email-otp-authenticator.jar

RUN chmod 644 /opt/keycloak/providers/email-otp-authenticator.jar \
    && chown keycloak:keycloak /opt/keycloak/providers/email-otp-authenticator.jar

# VeriVoto login theme (realm loginTheme=verivoto)
COPY --chown=keycloak:keycloak themes/verivoto /opt/keycloak/themes/verivoto

# Build-time options: PostgreSQL storage and health endpoints on the management port (9000)
ENV KC_DB=postgres
ENV KC_HEALTH_ENABLED=true

USER keycloak

RUN /opt/keycloak/bin/kc.sh build

ENTRYPOINT ["/opt/keycloak/bin/kc.sh"]
CMD ["start-dev"]