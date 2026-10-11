**Commit**: e3800e02f3 · **Ticket**: KAFKA-18843
**Subsystem**: connect/mirror
**Category**: Wrong serialization / deserialization

**Root cause**
Change the worker ID to the combination of the host name, flow and a random UUID to make it unique. Reviewers: Viktor Somogyi-Vass <viktorsomogyi@gmail.com>

**Before/After**
```java
// Before
    private final String advertisedUrl;
            this.advertisedUrl = internalServer.advertisedUrl().toString();
            this.advertisedUrl = "NOTUSED";
        String workerId = sourceAndTarget.toString();
                advertisedUrl, restClient, clientConfigOverridePolicy,

// After
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.UUID;
    private final URI advertisedUrl;
            this.advertisedUrl = internalServer.advertisedUrl();
            this.advertisedUrl = URI.create("NOTUSED");
        String workerId = generateWorkerId(sourceAndTarget);
                advertisedUrl.toString(), restClient, clientConfigOverridePolicy,
    private String generateWorkerId(SourceAndTarget sourceAndTarget) {
        if (config.enableInternalRest()) {
            return advertisedUrl.getHost() + ":" + advertisedUrl.getPort() + "/" + sourceAndTarget.toString();
```

**How to spot**
When reviewing connect/mirror code, look for wrong serialization / deserialization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18843: mirrormaker2 unique workerid (#18994).

**Test**
No test added.
