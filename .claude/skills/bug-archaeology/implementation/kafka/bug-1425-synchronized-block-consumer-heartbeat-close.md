**Commit**: d1db3d8e14 · **Ticket**: KAFKA-18805
**Subsystem**: clients/src
**Category**: Race condition / missing synchronization

**Root cause**
add synchronized block for Consumer Heartbeat close. Reviewers: Luke Chen <showuon@gmail.com>

**Before/After**
```java
// Before
                this.closed = true;

// After
                synchronized (AbstractCoordinator.this) {
                    this.closed = true;
                }
```

**How to spot**
When reviewing clients/src code, look for race condition / missing synchronization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18805: add synchronized block for consumer heartbeat close (#18920).

**Test**
No test added.
