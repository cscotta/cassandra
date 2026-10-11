**Commit**: 732ed0696b · **Ticket**: KAFKA-19190
**Subsystem**: clients/src
**Category**: Resource leak

**Root cause**
If the streams rebalance protocol is enabled in
StreamsUncaughtExceptionHandlerIntegrationTest, the streams application
does not shut down correctly upon error. There are two causes for this. First, sometimes, the SHUTDOWN_APPLICATION
code only sent with the leave heartbeat, but that is not handled broker
side. Second, the SHUTDOWN_APPLICATION code wasn't properly handled
client-side at all.

**Before/After**
```java
// Before
        if (statuses != null && !statuses.isEmpty()) {
            String statusDetails = statuses.stream()
                .map(status -> "(" + status.statusCode() + ") " + status.statusDetail())
                .collect(Collectors.joining(", "));
            logger.warn("Membership is in the following statuses: {}", statusDetails);

// After
        if (statuses != null) {
            streamsRebalanceData.setStatuses(statuses);
            if (!statuses.isEmpty()) {
                String statusDetails = statuses.stream()
                    .map(status -> "(" + status.statusCode() + ") " + status.statusDetail())
                    .collect(Collectors.joining(", "));
                logger.warn("Membership is in the following statuses: {}", statusDetails);
            }
import org.apache.kafka.common.message.StreamsGroupHeartbeatResponseData;
    private final AtomicReference<List<StreamsGroupHeartbeatResponseData.Status>> statuses = new AtomicReference<>(List.of());

    /** For the current stream thread to request a shutdown of all Streams clients belonging to the same application. */
```

**How to spot**
When reviewing clients/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19190: handle shutdown application correctly (#19544).

**Test**
Test modified in StreamsRebalanceDataTest.java covering the corrected behaviour.
