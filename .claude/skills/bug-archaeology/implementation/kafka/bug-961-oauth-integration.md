**Commit**: 7d6e5edf8e · **Ticket**: KAFKA-19153
**Subsystem**: build.gradle
**Category**: Logic error in condition

**Root cause**
Adds a test dependency on
[mock-oauth2-server](https://github.com/navikt/mock-oauth2-server/) for
integration tests for OAuth layer. Also includes fixes for some
regressions that were caught by the integration tests. Reviewers: Manikumar Reddy <manikumar@confluent.io>, Lianet Magrans
 <lmagrans@confluent.io>

**Before/After**
```java
// Before
                "clientId",
                "clientSecret",
                "scope",
        if (verificationKeyResolver == null || jwtValidator == null)

// After
    testImplementation libs.mockOAuth2Server
import static org.apache.kafka.common.config.SaslConfigs.SASL_OAUTHBEARER_CLIENT_CREDENTIALS_CLIENT_ID;
import static org.apache.kafka.common.config.SaslConfigs.SASL_OAUTHBEARER_CLIENT_CREDENTIALS_CLIENT_SECRET;
import static org.apache.kafka.common.config.SaslConfigs.SASL_OAUTHBEARER_SCOPE;
                SASL_OAUTHBEARER_CLIENT_CREDENTIALS_CLIENT_ID,
                SASL_OAUTHBEARER_CLIENT_CREDENTIALS_CLIENT_SECRET,
                SASL_OAUTHBEARER_SCOPE,
        if (jwtValidator == null)
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

    private static final Logger LOG = LoggerFactory.getLogger(ConfigurationUtils.class);
```

**How to spot**
When reviewing build.gradle code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19153: add oauth integration tests (#19938).

**Test**
Test modified in JwtBearerJwtRetrieverTest.java covering the corrected behaviour.
