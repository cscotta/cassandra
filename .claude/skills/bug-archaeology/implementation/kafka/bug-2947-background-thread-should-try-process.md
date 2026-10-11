**Commit**: 8b3c77c671 · **Ticket**: KAFKA-15305
**Subsystem**: clients/src
**Category**: Resource leak

**Root cause**
Reviewers: Lianet Magrans <lianetmr@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
        if (networkClientDelegate.unsentRequests().isEmpty())
        } while (timer.notExpired() && !networkClientDelegate.unsentRequests().isEmpty());

// After
        if (!networkClientDelegate.hasAnyPendingRequests())

        } while (timer.notExpired() && networkClientDelegate.hasAnyPendingRequests());
    /**
     * Return true if there is at least one in-flight request or unsent request.
     */
    public boolean hasAnyPendingRequests() {
        return client.hasInFlightRequests() || !unsentRequests.isEmpty();
    }

    @Test
    void testSendUnsentRequest() {
```

**How to spot**
When reviewing clients/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-15305 the background thread should try to process the remaining task until the shutdown timer is expired. (#16156).

**Test**
Test modified in ConsumerNetworkThreadTest.java covering the corrected behaviour.
