**Commit**: cfc7bb90ae · **Ticket**: KAFKA-16345
**Subsystem**: clients/src
**Category**: Wrong serialization / deserialization

**Root cause**
Reviewers: Mickael Maison <mickael.maison@gmail.com>, Kirk True <kirk@kirktrue.pro>

**Before/After**
```java
// Before
                .define(SaslConfigs.SASL_OAUTHBEARER_EXPECTED_ISSUER, ConfigDef.Type.STRING, null, ConfigDef.Importance.LOW, SASL_OAUTHBEARER_EXPECTED_ISSUER_DOC);
                cu.validateInteger(SASL_LOGIN_READ_TIMEOUT_MS, false));

// After
    public static final String SASL_OAUTHBEARER_HEADER_URLENCODE = "sasl.oauthbearer.header.urlencode";
    public static final boolean DEFAULT_SASL_OAUTHBEARER_HEADER_URLENCODE = false;
    public static final String SASL_OAUTHBEARER_HEADER_URLENCODE_DOC = "The (optional) setting to enable the OAuth client to URL-encode the client_id and client_secret in the authorization header"
            + " in accordance with RFC6749, see <a href=\"https://datatracker.ietf.org/doc/html/rfc6749#section-2.3.1\">here</a> for more details. The default value is set to 'false' for backward compatibility";
                .define(SaslConfigs.SASL_OAUTHBEARER_EXPECTED_ISSUER, ConfigDef.Type.STRING, null, ConfigDef.Importance.LOW, SASL_OAUTHBEARER_EXPECTED_ISSUER_DOC)
                .define(SaslConfigs.SASL_OAUTHBEARER_HEADER_URLENCODE, ConfigDef.Type.BOOLEAN, DEFAULT_SASL_OAUTHBEARER_HEADER_URLENCODE, ConfigDef.Importance.LOW, SASL_OAUTHBEARER_HEADER_URLENCODE_DOC);
import static org.apache.kafka.common.config.SaslConfigs.SASL_OAUTHBEARER_HEADER_URLENCODE;
                cu.validateInteger(SASL_LOGIN_READ_TIMEOUT_MS, false),
                cu.get(SASL_OAUTHBEARER_HEADER_URLENCODE));
    private final boolean urlencodeHeader;

```

**How to spot**
When reviewing clients/src code, look for wrong serialization / deserialization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16345: optionally url-encode clientid and clientsecret in authorization header (#15475).

**Test**
Test modified in HttpAccessTokenRetrieverTest.java covering the corrected behaviour.
