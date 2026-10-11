**Commit**: a0292bae39 · **Ticket**: KAFKA-17463
**Subsystem**: share/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Andrew Schofield <aschofield@confluent.io>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
    public void testWriteStateRequestFailButCoordinatorSuccess() {
                                .setErrorCode(Errors.NOT_COORDINATOR.code())
                                .setErrorMessage(Errors.NOT_COORDINATOR.message())
        WriteShareGroupStateResponse result = null;
        try {
            result = resultFuture.get();
        } catch (Exception e) {
            fail("Failed to get result from future", e);
        }
        verify(handler, times(1)).findShareCoordinatorBuilder();

// After
                case NOT_COORDINATOR:
                            case NOT_COORDINATOR:
                            case NOT_COORDINATOR:
    public void testWriteStateRequestRetryWithNotCoordinatorSuccessfulOnRetry() throws InterruptedException, ExecutionException {
        client.prepareResponseFrom(body -> body instanceof FindCoordinatorRequest
                && ((FindCoordinatorRequest) body).data().keyType() == FindCoordinatorRequest.CoordinatorType.SHARE.id()
                && ((FindCoordinatorRequest) body).data().coordinatorKeys().get(0).equals(coordinatorKey),
            new FindCoordinatorResponse(
                new FindCoordinatorResponseData()
                    .setCoordinators(Collections.singletonList(
                        new FindCoordinatorResponseData.Coordinator()
                            .setErrorCode(Errors.NOT_COORDINATOR.code())
```

**How to spot**
When reviewing share/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17463 add retry in persister for not_coordinator error (#17645).

**Test**
Test modified in PersisterStateManagerTest.java covering the corrected behaviour.
