**Commit**: a26b2fd5fe · **Ticket**: KAFKA-18608
**Subsystem**: docs/security
**Category**: Logic error in condition

**Root cause**
- Add documentation for client assertion authentication support in the
client_credentials grant type, introduced in
https://cwiki.apache.org/confluence/display/KAFKA/KIP-1258 / PR #21483
- Document the three-tier fallback mechanism (file-based assertion >
locally-generated assertion > client secret)
- Add configuration examples for dynamically-generated assertions,
pre-generated assertion files, and assertion template files
- Clarify that DefaultJwtRetriever auto-delegates to
`ClientCredentialsJwtRetriever` for HTTP/HTTPS endpoints, so
`sasl.oauthbearer.jwt.retriever.class` does not need to be set
explicitly in most cases
- Update "Secure/Production Use" section to reference built-in JWT
ret...

**Before/After**
```java
// Before
2. Configure the following properties in producer.properties or consumer.properties. For example, if using the OAuth `client_credentials` grant type to communicate with the OAuth identity provider, the configuration might look like this:

               sasl.oauthbearer.jwt.retriever.class=org.apache.kafka.common.security.oauthbearer.ClientCredentialsJwtRetriever
Or, if using the OAuth `urn:ietf:params:oauth:grant-type:jwt-bearer` grant type to communicate with the OAuth identity provider, the configuration might look like this:


// After
2. Configure the following properties in producer.properties or consumer.properties. The `sasl.oauthbearer.jwt.retriever.class` property defaults to `DefaultJwtRetriever`, which automatically delegates to `ClientCredentialsJwtRetriever` for HTTP/HTTPS token endpoint URLs and `FileJwtRetriever` for `file://` URLs. In most cases, this property does not need to be set explicitly.

For example, if using the OAuth `client_credentials` grant type with a client secret to communicate with the OAuth identity provider, the configuration might look like this:

Alternatively, the `client_credentials` grant type also supports client assertion authentication as defined in [RFC 7523](https://tools.ietf.org/html/rfc7523). Instead of sending a client secret, the client authenticates by presenting a signed JWT assertion to the OAuth identity provider. This provides enhanced security since private keys never leave the client and assertions are short-lived. See [KIP-1258](https://cwiki.apache.org/confluence/display/KAFKA/KIP-1258) for details.

When using client assertion with dynamically-generated JWTs (recommended), the configuration might look like this:

               security.protocol=SASL_SSL
               sasl.mechanism=OAUTHBEARER
               sasl.oauthbearer.token.endpoint.url=https://example.com/oauth2/v1/token
               sasl.oauthbearer.assertion.private.key.file=/path/to/private-key.pem
```

**How to spot**
When reviewing docs/security code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular:  kafka-18608: add documentation for oauth client assertion authentication (kip-1258) (#21859).

**Test**
No test added.
