**Commit**: 08057eac53 · **Ticket**: KAFKA-18600
**Subsystem**: clients/src
**Category**: State not cleaned up

**Root cause**
This PR removes associated logging within NetworkClient to reduce noise
and streamline the client code. Reviewers: Ismael Juma <ismael@juma.me.uk>, David Arthur
 <mumrah@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
        log.debug("Node {} has finalized features epoch: {}, finalized features: {}, supported features: {}, ZK migration ready: {}, API versions: {}.",
                apiVersionsResponse.data().supportedFeatures(), apiVersionsResponse.data().zkMigrationReady(), nodeVersionInfo);

// After
        log.debug("Node {} has finalized features epoch: {}, finalized features: {}, supported features: {}, API versions: {}.",
                apiVersionsResponse.data().supportedFeatures(), nodeVersionInfo);
```

**How to spot**
When reviewing clients/src code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18600 cleanup networkclient zk related logging (#18644).

**Test**
No test added.
