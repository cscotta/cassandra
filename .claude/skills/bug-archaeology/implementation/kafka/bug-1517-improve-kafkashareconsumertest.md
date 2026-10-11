**Commit**: f960e20647 · **Ticket**: KAFKA-18488
**Subsystem**: clients/src
**Category**: Resource leak

**Root cause**
Reviewers: Lianet Magrans <lmagrans@confluent.io>

**Before/After**
```java
// Before
                    time,
            ShareGroupHeartbeatRequest request = (ShareGroupHeartbeatRequest) body;
            memberId.set(Uuid.fromString(request.data().memberId()));
            boolean matches = request.data().memberEpoch() == 0;
            heartbeatsReceived.addAndGet(1);

            client.prepareResponseFrom(body2 -> {
                ShareGroupHeartbeatRequest request2 = (ShareGroupHeartbeatRequest) body2;
                boolean matches2 = request2.data().memberId().equals(memberId.get().toString()) && request2.data().memberEpoch() == 1;
                return matches2;
            }, shareGroupHeartbeatResponse(memberId.get(), 2, ti1p0), coordinator);
            return matches;

// After
                        time,
import org.apache.kafka.common.test.api.Flaky;
            if (body instanceof ShareGroupHeartbeatRequest) {
                ShareGroupHeartbeatRequest request = (ShareGroupHeartbeatRequest) body;
                memberId.set(Uuid.fromString(request.data().memberId()));
                boolean matches = request.data().memberEpoch() == 0;
                client.prepareResponseFrom(body2 -> {
                    if (body2 instanceof ShareGroupHeartbeatRequest) {
                        ShareGroupHeartbeatRequest request2 = (ShareGroupHeartbeatRequest) body2;
                        boolean matches2 = request2.data().memberId().equals(memberId.get().toString()) && request2.data().memberEpoch() == 1;
                        heartbeatsReceived.addAndGet(1);
                        return matches2;
```

**How to spot**
When reviewing clients/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18488: improve kafkashareconsumertest (#18728).

**Test**
Test modified in KafkaShareConsumerTest.java covering the corrected behaviour.
