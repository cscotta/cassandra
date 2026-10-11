**Commit**: c207438823 · **Ticket**: KAFKA-17279
**Subsystem**: clients/src
**Category**: Resource leak

**Root cause**
Handle retriable errors from offset fetches in ConsumerCoordinator. Reviewers: Lianet Magrans <lianetmr@gmail.com>, David Jacot <djacot@confluent.io>

**Before/After**
```java
// Before
                if (responseError == Errors.COORDINATOR_LOAD_IN_PROGRESS) {
                    // just retry
                    future.raise(responseError);
                } else if (responseError == Errors.NOT_COORDINATOR) {
    @Test
    public void testRefreshOffsetLoadInProgress() {
        client.prepareResponse(groupCoordinatorResponse(node, Errors.NONE));
        coordinator.ensureCoordinatorReady(time.timer(Long.MAX_VALUE));

        subscriptions.assignFromUser(singleton(t1p));
        client.prepareResponse(offsetFetchResponse(Errors.COORDINATOR_LOAD_IN_PROGRESS, Collections.emptyMap()));
        client.prepareResponse(offsetFetchResponse(t1p, Errors.NONE, "", 100L));

// After
                if (responseError == Errors.COORDINATOR_NOT_AVAILABLE ||
                    responseError == Errors.NOT_COORDINATOR) {
                } else if (responseError.exception() instanceof RetriableException) {
                    // retry
                    future.raise(responseError);
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
    @ParameterizedTest
    @CsvSource({
        "NOT_COORDINATOR, true",
        "COORDINATOR_NOT_AVAILABLE, true",
        "COORDINATOR_LOAD_IN_PROGRESS, false",
```

**How to spot**
When reviewing clients/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17279: handle retriable errors from offset fetches (#16826).

**Test**
Test modified in ConsumerCoordinatorTest.java covering the corrected behaviour.
