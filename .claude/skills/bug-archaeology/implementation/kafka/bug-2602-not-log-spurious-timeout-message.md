**Commit**: 4763d0fc30 · **Ticket**: KAFKA-17242
**Subsystem**: connect/mirror
**Category**: Logic error in condition

**Root cause**
Reviewers: Chris Egerton <chrise@aiven.io>

**Before/After**
```java
// Before
        scheduler.execute(() -> {
        executor.scheduleAtFixedRate(() -> executeThread(task, description), 0, interval.toMillis(), TimeUnit.MILLISECONDS);
        executor.scheduleAtFixedRate(() -> executeThread(task, description), interval.toMillis(),
            executor.submit(() -> executeThread(task, description)).get(timeout.toMillis(), TimeUnit.MILLISECONDS);
    private void run(Task task, String description) {
            if (elapsed > timeout.toMillis()) {
    private void executeThread(Task task, String description) {
        run(task, description);

// After
        scheduler.executeAsync(() -> {
        executor.scheduleAtFixedRate(() -> executeThread(task, description, true), 0, interval.toMillis(), TimeUnit.MILLISECONDS);
        executor.scheduleAtFixedRate(() -> executeThread(task, description, true), interval.toMillis(),
            executor.submit(() -> executeThread(task, description, true)).get(timeout.toMillis(), TimeUnit.MILLISECONDS);
    void executeAsync(Task task, String description) {
        executor.submit(() -> executeThread(task, description, false));
    }

    private void run(Task task, String description, boolean checkTimeout) {
            if (checkTimeout && elapsed > timeout.toMillis()) {
    private void executeThread(Task task, String description, boolean checkTimeout) {
        run(task, description, checkTimeout);
```

**How to spot**
When reviewing connect/mirror code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17242: do not log spurious timeout message for mirrorcheckpointtask sync store startup (#16773).

**Test**
No test added.
