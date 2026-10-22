FROM quay.io/keycloak/keycloak:26.8.1

ADD --checksum=sha256:8013758fe4f24dfb561d57f83c0853387fb18c9b35925c77fb60ccd9af7cedea \
    https://github.com/for-keycloak/email-otp-authenticator/releases/download/v1.8.1/email-otp-authenticator-v1.8.1-kc-26.8.1.jar \
    /opt/keycloak/providers/email-otp-authenticator.jar

RUN /opt/keycloak/bin/kc.sh build

ENTRYPOINT ["/opt/keycloak/bin/kc.sh"]
CMD ["start-dev"]
