**Commit**: 861eeb859d · **Ticket**: KAFKA-19295
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Remove the event IDs from the ApplicationEvent and BackgroundEvent as it
serves no functional purpose other than uniquely identifying events in
the logs. Reviewers: Andrew Schofield <aschofield@confluent.io>

**Before/After**
```java
// Before
import org.apache.kafka.common.Uuid;
    /**
     * This identifies a particular event. It is used to disambiguate events via {@link #hashCode()} and
     * {@link #equals(Object)} and can be used in log messages when debugging.
     */
    private final Uuid id;

        this.id = Uuid.randomUuid();
    public Uuid id() {
        return id;
    }


// After
        return "type=" + type + ", enqueuedMs=" + enqueuedMs;
        return "type=" + type + ", enqueuedMs=" + enqueuedMs;
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19295: remove asynckafkaconsumer event id generation (#19915).

**Test**
No test added.
