**Commit**: d3707fc815 · **Ticket**: KAFKA-19214
**Subsystem**: clients/src
**Category**: State not cleaned up

**Root cause**
Change:

`public List<Optional<? extends RequestManager>> entries();`

to:

`public List<RequestManager> entries();`

and clean up the callers. Reviewers: TengYao Chi <kitingiao@gmail.com>, Andrew Schofield
 <aschofield@confluent.io>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
import java.util.Optional;
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(networkClientDelegate::addAll)
                .reduce(MAX_POLL_TIMEOUT_MS, Math::min);
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(rm -> rm.maximumTimeToWait(currentTimeMs))
                .reduce(Long.MAX_VALUE, Math::min);
    static void runAtClose(final Collection<Optional<? extends RequestManager>> requestManagers,
                .filter(Optional::isPresent)
                .map(Optional::get)

// After
                .mapToLong(networkClientDelegate::addAll)
                .filter(ms -> ms <= MAX_POLL_TIMEOUT_MS)
                .min()
                .orElse(MAX_POLL_TIMEOUT_MS);

                .mapToLong(rm -> rm.maximumTimeToWait(currentTimeMs))
                .min()
                .orElse(Long.MAX_VALUE);
    static void runAtClose(final Collection<RequestManager> requestManagers,
    private final List<RequestManager> entries;
        List<RequestManager> list = new ArrayList<>();
        coordinatorRequestManager.ifPresent(list::add);
```

**How to spot**
When reviewing clients/src code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19214: clean up use of optionals in requestmanagers.entries() (#19609).

**Test**
Test modified in ConsumerNetworkThreadTest.java covering the corrected behaviour.
