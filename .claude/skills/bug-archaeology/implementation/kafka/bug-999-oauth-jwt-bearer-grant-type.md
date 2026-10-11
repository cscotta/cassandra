**Commit**: 1e917906ab · **Ticket**: KAFKA-18573
**Subsystem**: checkstyle/import-control.xml
**Category**: Logic error in condition

**Root cause**
Adding support for the `urn:ietf:params:oauth:grant-type:jwt-bearer`
grant type (AKA `jwt-bearer`). Includes further refactoring of the
existing OAuth layer and addition of generic JWT assertion layer that
can be leveraged in the future. This constitutes the main piece of the JWT Bearer grant type support. Forthcoming commits/PRs will include improvements for both the
`client_credentials` and `jwt-bearer` grant types in the following
areas:

* Integration test coverage (KAFKA-19153)
* Unit test coverage (KAFKA-19308)
* Top-level documentation (KAFKA-19152)
* Improvements to and documentation for `OAuthCompatibilityTool`
(KAFKA-19307)

Reviewers: Manikumar Reddy <manikumar@confluent.io>, Lian...

**Before/After**
```java
// Before
// (no removals)

// After
        <allow pkg="javax.crypto"/>
import org.apache.kafka.common.config.ConfigDef.CaseInsensitiveValidString;
    public static final String SASL_OAUTHBEARER_JWT_RETRIEVER_CLASS = "sasl.oauthbearer.jwt.retriever.class";
    public static final String DEFAULT_SASL_OAUTHBEARER_JWT_RETRIEVER_CLASS = "org.apache.kafka.common.security.oauthbearer.DefaultJwtRetriever";
    public static final String SASL_OAUTHBEARER_JWT_RETRIEVER_CLASS_DOC = "<p>The fully-qualified class name of a <code>JwtRetriever</code> implementation used to"
        + " request tokens from the identity provider.</p>"
        + "<p>The default configuration value represents a class that maintains backward compatibility with previous versions of"
        + " Apache Kafka. The default implementation uses the configuration to determine which concrete implementation to create."
        + "<p>Other implementations that are provided include:</p>"
        + "<ul>"
        + "<li><code>org.apache.kafka.common.security.oauthbearer.ClientCredentialsJwtRetriever</code></li>"
        + "<li><code>org.apache.kafka.common.security.oauthbearer.DefaultJwtRetriever</code></li>"
```

**How to spot**
When reviewing checkstyle/import-control.xml code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18573: add support for oauth jwt-bearer grant type (#19754).

**Test**
Test modified in BrokerJwtValidatorTest.java covering the corrected behaviour.
