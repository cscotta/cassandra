**Commit**: dfced692d2 · **Ticket**: KAFKA-19551
**Subsystem**: storage/src
**Category**: Logic error in condition

**Root cause**
FatalExitError is not thrown after
[KAFKA-19425](https://issues.apache.org/jira/browse/KAFKA-19425). Clean
up the handling of FatalExitError in `RemoteStorageThreadPool`.

**Before/After**
```java
// Before
import org.apache.kafka.common.internals.FatalExitError;
import org.apache.kafka.common.utils.Exit;
        if (th != null) {
            if (th instanceof FatalExitError) {
                LOGGER.error("Stopping the server as it encountered a fatal error.");
                Exit.exit(((FatalExitError) th).statusCode());
            } else {
                if (!isShutdown())
                    LOGGER.error("Error occurred while executing task: {}", runnable, th);
            }

// After
        if (th != null && !isShutdown()) {
            LOGGER.error("Error occurred while executing task: {}", runnable, th);
```

**How to spot**
When reviewing storage/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19551: remove the handling of fatalexiterror in remotestoragethreadpool (#20245).

**Test**
Test modified in RemoteStorageThreadPool.java covering the corrected behaviour.
