**Commit**: b4eb7cf390 · **Ticket**: KAFKA-16683
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@apache.org>

**Before/After**
```java
// Before
// (no removals)

// After
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.common.network.ConnectionMode;
import org.apache.kafka.common.security.auth.SecurityProtocol;
import org.apache.kafka.test.TestSslUtils;
    public static final String SSL_CERTIFICATE_CN = "localhost";


    public static boolean usesSslTransportLayer(SecurityProtocol securityProtocol) {
        switch (securityProtocol) {
            case SSL:
            case SASL_SSL:
                return true;
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16683 extract security-related helpers from scala.testutils to java class (#16912).

**Test**
Test modified in JaasTestUtils.java covering the corrected behaviour.
