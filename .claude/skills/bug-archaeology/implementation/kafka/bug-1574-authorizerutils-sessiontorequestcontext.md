**Commit**: 0b07db572b · **Ticket**: KAFKA-18595
**Subsystem**: server/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Christo Lolov <lolovc@amazon.com>

**Before/After**
```java
// Before
import org.apache.kafka.common.security.auth.KafkaPrincipal;
import org.apache.kafka.common.security.auth.SecurityProtocol;
import org.apache.kafka.network.Session;
import org.apache.kafka.server.authorizer.AuthorizableRequestContext;
import java.net.InetAddress;


    public static AuthorizableRequestContext sessionToRequestContext(Session session) {
        return new AuthorizableRequestContext() {
            @Override
            public String clientId() {
                return "";

// After
// (no additions)
```

**How to spot**
When reviewing server/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18595: remove authorizerutils#sessiontorequestcontext (#18631).

**Test**
No test added.
