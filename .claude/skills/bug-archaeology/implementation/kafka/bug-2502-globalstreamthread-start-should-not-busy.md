**Commit**: 9eb7e1a23d · **Ticket**: KAFKA-17100
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
This PR replaces a busy-wait sleep with a CountDownLatch. Reviewers: Greg Harris <greg.harris@aiven.io>, Matthias J. Sax <matthias@confluent.io>

**Before/After**
```java
// Before
import org.apache.kafka.common.utils.Utils;
import static org.apache.kafka.streams.processor.internals.GlobalStreamThread.State.CREATED;
    public boolean stillInitializing() {
        synchronized (stateLock) {
            return state.equals(CREATED);
        }
    }

        while (stillInitializing()) {
            Utils.sleep(1);
            if (startupException != null) {
                throw startupException;

// After
import java.util.concurrent.CountDownLatch;
    private final CountDownLatch initializationLatch = new CountDownLatch(1);
        } finally {
            initializationLatch.countDown();
        try {
            initializationLatch.await();
        } catch (final InterruptedException e) {
            currentThread().interrupt();
            throw new IllegalStateException("GlobalStreamThread was interrupted during initialization", e);
        }

        if (startupException != null) {
```

**How to spot**
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17100: globalstreamthread#start should not busy-wait (#16914).

**Test**
No test added.
