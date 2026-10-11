**Commit**: eda8fc84ae · **Ticket**: KAFKA-16918
**Subsystem**: clients/src
**Category**: Race condition / missing synchronization

**Root cause**
Reviewers: TengYao Chi <kitingiao@gmail.com>, Luke Chen <showuon@gmail.com>, Parker Chang <45290853+Parkerhiphop@users.noreply.github.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
import static org.junit.jupiter.api.Assertions.assertThrows;
     * Assert that a future raises an expected exception cause type. Return the exception cause
     * if the assertion succeeds; otherwise raise AssertionError.
        ExecutionException exception = assertThrows(ExecutionException.class, future::get);
        Throwable cause = exception.getCause();

        // Enable strict type checking.
        // This ensures we're testing for the exact exception type, not its subclasses.
        assertEquals(exceptionCauseClass, cause.getClass(),
            "Expected a " + exceptionCauseClass.getSimpleName() + " exception, but got " +
                        cause.getClass().getSimpleName());
        return exceptionCauseClass.cast(exception.getCause());

// After
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
     * Assert that a future raises an expected exception cause type.
     * This method will wait for the future to complete or timeout(15000 milliseconds).
        try {
            future.get(DEFAULT_MAX_WAIT_MS, TimeUnit.MILLISECONDS);
            fail("Should throw expected exception " + exceptionCauseClass.getSimpleName() + " but nothing was thrown.");
        } catch (InterruptedException | ExecutionException | CancellationException e) {
            Throwable cause = e instanceof ExecutionException ? e.getCause() : e;
            // Enable strict type checking.
            // This ensures we're testing for the exact exception type, not its subclasses.
```

**How to spot**
When reviewing clients/src code, look for race condition / missing synchronization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16918 testutils#assertfuturethrows should use future.get with timeout  (#18891).

**Test**
Test modified in TestUtils.java covering the corrected behaviour.
