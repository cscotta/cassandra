**Commit**: 247132569e · **Ticket**: KAFKA-16912
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
import java.util.ArrayList;
import static org.apache.kafka.clients.consumer.internals.ConsumerTestBuilder.DEFAULT_REQUEST_TIMEOUT_MS;
    @Test
    void testPollResultTimer() {
        NetworkClientDelegate.UnsentRequest req = new NetworkClientDelegate.UnsentRequest(
                new FindCoordinatorRequest.Builder(
                        new FindCoordinatorRequestData()
                                .setKeyType(FindCoordinatorRequest.CoordinatorType.TRANSACTION.id())
                                .setKey("foobar")),
                Optional.empty());
        req.setTimer(time, DEFAULT_REQUEST_TIMEOUT_MS);


// After
import java.util.ArrayList;
    private static final long DEFAULT_REQUEST_TIMEOUT_MS = 500;
    @Test
    void testPollResultTimer() throws Exception {
        try (NetworkClientDelegate ncd = newNetworkClientDelegate()) {
            NetworkClientDelegate.UnsentRequest req = new NetworkClientDelegate.UnsentRequest(
                    new FindCoordinatorRequest.Builder(
                            new FindCoordinatorRequestData()
                                    .setKeyType(FindCoordinatorRequest.CoordinatorType.TRANSACTION.id())
                                    .setKey("foobar")),
                    Optional.empty());
            req.setTimer(time, DEFAULT_REQUEST_TIMEOUT_MS);
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16912 migrate consumernetworkthreadtest.testpollresulttimer() to networkclientdelegatetest (#16234).

**Test**
Test modified in ConsumerNetworkThreadTest.java covering the corrected behaviour.
