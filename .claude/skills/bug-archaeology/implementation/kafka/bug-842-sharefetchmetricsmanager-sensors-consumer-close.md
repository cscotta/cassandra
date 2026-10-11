**Commit**: 40b4fdb0d8 · **Ticket**: KAFKA-19559
**Subsystem**: clients/src
**Category**: Resource leak

**Root cause**
*What*
https://issues.apache.org/jira/browse/KAFKA-19559

- There are a few sensors(created when a `ShareConsumer` initializes)
which are not removed when the `ShareConsumer` closes. - To maintain consistency and prevent any leaks, we have to remove all
the sensors when the consumer is closed. This is similar to this issue for regular consumers -
https://issues.apache.org/jira/browse/KAFKA-19542

Reviewers: Andrew Schofield <aschofield@confluent.io>, Apoorv Mittal
<apoorvmittal10@gmail.com>

**Before/After**
```java
// Before
public class ShareFetchMetricsManager {

// After
        Utils.closeQuietly(metricsManager, "shareFetchMetricsManager");
import java.io.IOException;
import java.util.Arrays;

public class ShareFetchMetricsManager implements AutoCloseable {

    @Override
    public void close() throws IOException {
        Arrays.asList(
            throttleTime.name(),
            bytesFetched.name(),
            recordsFetched.name(),
```

**How to spot**
When reviewing clients/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19559: remove sharefetchmetricsmanager sensors on consumer.close() (#20255).

**Test**
Test modified in ShareConsumeRequestManagerTest.java covering the corrected behaviour.
