FROM quay.io/keycloak/keycloak:26.8.1

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