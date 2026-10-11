**Commit**: 6cffed56f6 · **Ticket**: KAFKA-16943
**Subsystem**: connect/runtime
**Category**: Logic error in condition

**Root cause**
Reviewers: Chris Egerton <chrise@aiven.io>

**Before/After**
```java
// Before
        this.herderExecutor.submit(this);
        // the status topic may have been

// After
import org.apache.kafka.connect.runtime.distributed.DistributedHerder;
import java.util.concurrent.Future;
    private Future<?> herderTask;
    /**
     * Track task status which have been submitted to work thread.
     * @return {@link DistributedHerder#herderTask} to track status or null if the herder type doesn't have a separate work thread
     */
    public Future<?> herderTask() {
        return this.herderTask;
    }

            if (herder instanceof DistributedHerder) {
```

**How to spot**
When reviewing connect/runtime code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16943: synchronously verify connect worker startup failure in internaltopicsintegrationtest (#16451).

**Test**
Test modified in InternalTopicsIntegrationTest.java covering the corrected behaviour.
