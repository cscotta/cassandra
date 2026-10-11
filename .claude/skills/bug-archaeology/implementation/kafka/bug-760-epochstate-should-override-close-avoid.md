**Commit**: 8a79ea2e5b · **Ticket**: KAFKA-19676
**Subsystem**: raft/src
**Category**: Resource leak

**Root cause**
Jira: [KAFKA-19676](https://issues.apache.org/jira/browse/KAFKA-19676)
All subclasses of EpochState do not throw an IOException when closing,
so catching it is unnecessary. We could override close to remove the
IOException declaration. Reviewers: Jhen-Yung Hsu <jhenyunghsu@gmail.com>, TaiJuWu
 <tjwu1217@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
import java.io.IOException;
import java.io.UncheckedIOException;
            try {
                state.close();
            } catch (IOException e) {
                throw new UncheckedIOException(
                    "Failed to transition from " + state.name() + " to " + newState.name(), e);
            }

// After

    /**
     * Since all subclasses implement the Closeable interface while none throw any IOException,
     * this implementation is provided to eliminate the need for exception handling in the close operation.
     */
    @Override
    void close();
            state.close();
```

**How to spot**
When reviewing raft/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19676 epochstate should override close to avoid throwing ioexception (#20508).

**Test**
No test added.
