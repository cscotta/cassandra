**Commit**: 4ebf018a5a · **Ticket**: KAFKA-18608
**Subsystem**: build.gradle
**Category**: Logic error in condition

**Root cause**
## What

Implements

[KIP-1258](https://cwiki.apache.org/confluence/display/KAFKA/KIP-1258%3A+Add+Support+for+OAuth+Client+Assertion+to+client_credentials+Grant+Type):
Add support for OAuth 2.0 client assertion authentication (RFC 7523§2.2)
as a more secure alternative to client secrets. ## Key Changes

### Core Implementation
- **New**: `ClientAssertionRequestFormatter` - Formats HTTP requests
with client assertion parameters
- **Enhanced**: `ClientCredentialsRequestFormatterFactory` - Three-tier
fallback mechanism with logging
- **Renamed**: `ClientCredentialsRequestFormatter`
→`ClientSecretRequestFormatter` (internal class)

### Three-Tier Fallback
1. File-based assertion (`sasl.oauthbear...

**Before/After**
```java
// Before
import org.apache.kafka.common.config.ConfigException;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.security.oauthbearer.internals.secured.ClientCredentialsRequestFormatter;
import java.util.Objects;
import java.util.function.Function;
import static org.apache.kafka.common.config.SaslConfigs.DEFAULT_SASL_OAUTHBEARER_HEADER_URLENCODE;
import static org.apache.kafka.common.config.SaslConfigs.SASL_JAAS_CONFIG;
import static org.apache.kafka.common.config.SaslConfigs.SASL_OAUTHBEARER_CLIENT_CREDENTIALS_CLIENT_ID;
import static org.apache.kafka.common.config.SaslConfigs.SASL_OAUTHBEARER_CLIENT_CREDENTIALS_CLIENT_SECRET;
import static org.apache.kafka.common.config.SaslConfigs.SASL_OAUTHBEARER_HEADER_URLENCODE;
imp

// After
    testImplementation libs.testcontainersKeycloak
    testImplementation libs.testcontainersJunitJupiter
        <allow pkg="org.testcontainers" />
        <allow pkg="org.keycloak" />
        <allow pkg="dasniko.testcontainers" />
import org.apache.kafka.common.security.oauthbearer.internals.secured.ClientCredentialsRequestFormatterFactory;
import org.apache.kafka.common.utils.Time;
```

**How to spot**
When reviewing build.gradle code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18608: add support for oauth client assertion to client_credentials grant type (#21483).

**Test**
Test modified in ClientAssertionKeycloakIntegrationTest.java covering the corrected behaviour.
