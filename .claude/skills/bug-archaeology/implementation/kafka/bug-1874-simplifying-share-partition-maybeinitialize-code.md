**Commit**: ee4264439d · **Ticket**: KAFKA-18129
**Subsystem**: checkstyle/suppressions.xml
**Category**: Resource leak

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
    <suppress checks="CyclomaticComplexity|ClassDataAbstractionCoupling" files="SharePartition.java"/>
import java.util.concurrent.atomic.AtomicReference;
        CompletableFuture<Void> future = new CompletableFuture<>();
        AtomicReference<Optional<Throwable>> futureException = new AtomicReference<>(Optional.empty());
        InitializationResult initializationResult = checkInitializationCompletion();
        if (initializationResult.isComplete()) {
            if (initializationResult.throwable() != null) {
                future.completeExceptionally(initializationResult.throwable());
            } else {
                future.complete(null);
            }
            return future;

// After
    <suppress checks="CyclomaticComplexity" files="SharePartition.java"/>
        try {
            if (initializedOrThrowException()) return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        // If code reaches here then the share partition is not initialized. Initialize the share partition.
        // Attain lock while updating the state to avoid any concurrent requests to be processed.
            if (!emptyToInitialState()) return CompletableFuture.completedFuture(null);
            return CompletableFuture.failedFuture(e);

        // The share partition is not initialized, hence try to initialize it. There shall be only one
        // request trying to initialize the share partition.
```

**How to spot**
When reviewing checkstyle/suppressions.xml code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18129 simplifying share partition maybeinitialize code (#18093).

**Test**
No test added.
