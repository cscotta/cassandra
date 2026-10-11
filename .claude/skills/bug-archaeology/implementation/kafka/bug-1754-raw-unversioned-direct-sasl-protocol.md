**Commit**: 875da35ec3 · **Ticket**: KAFKA-18339
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Clients that support SASL but don't implement KIP-43 (eg Kafka producer/consumer 0.9.0.x) will
fail to connect after this change. Added unit tests and also manually tested with the console producer 0.9.0. While testing, I noticed that the logged message when a 0.9.0 Java client is used without sasl is
slightly misleading - fixed that too. Reviewers: Manikumar Reddy <manikumar.reddy@gmail.com>

**Before/After**
```java
// Before
        INITIAL_REQUEST,               // May be GSSAPI token, SaslHandshake or ApiVersions for authentication
                case INITIAL_REQUEST:
                    if (handleKafkaRequest(clientToken))
                        break;
                    // For default GSSAPI, fall through to authenticate using the client token as the first GSSAPI packet.
                    // This is required for interoperability with 0.9.0.x clients which do not send handshake request
    private boolean handleKafkaRequest(byte[] requestBytes) throws IOException, AuthenticationException {
        boolean isKafkaRequest = false;
        String clientMechanism = null;
            // A valid Kafka request header was received. SASL authentication tokens are now expected only
            // following a SaslHandshakeRequest since this is not a GSSAPI client token from a Kafka 0.9.0.x client.
            if (saslState == SaslState.INITIAL_REQUEST)

// After
        INITIAL_REQUEST,               // May be SaslHandshake or ApiVersions for authentication
                case INITIAL_REQUEST:
    /**
     * @throws InvalidRequestException if the request is not in Kafka format or if the API key is invalid. Clients
     * that support SASL without support for KIP-43 (e.g. Kafka Clients 0.9.x) are in the former bucket - the first
     * packet such clients send is a GSSAPI token starting with 0x60.
     */
    private void handleKafkaRequest(byte[] requestBytes) throws IOException, AuthenticationException {
                throw new InvalidRequestException("Unexpected Kafka request of type " + apiKey + " during SASL handshake.");

            // A valid Kafka request was received, we can now update the sasl state
            if (saslState == SaslState.INITIAL_REQUEST)
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18339: remove raw unversioned direct sasl protocol (kip-896) (#18295).

**Test**
Test modified in SaslServerAuthenticatorTest.java covering the corrected behaviour.
