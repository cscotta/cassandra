**Commit**: 7413a5a9e4 · **Ticket**: KAFKA-17913
**Subsystem**: docs/ops.html
**Category**: Race condition / missing synchronization

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
  <p>A Kafka admin will typically select 3 servers for this role, depending on factors like cost and the number of concurrent failures your system should withstand without availability impact. A majority of the controllers must be alive in order to maintain availability. With 3 controllers, the cluster can tolerate 1 controller failure; for configurations with more than 3 controllers, see <a href="#kraft_deployment">Deploying Considerations</a> for more details.</p>
    <li>For redundancy, a Kafka cluster should use 3 controllers. More than 3 controllers is not recommended in critical environments. In the rare case of a partial network failure it is possible for the cluster metadata quorum to become unavailable. This limitation will be addressed in a future release of Kafka.</li>

// After
  <p>A Kafka admin will typically select 3 or 5 servers for this role, depending on factors like cost and the number of concurrent failures your system should withstand without availability impact. A majority of the controllers must be alive in order to maintain availability. With 3 controllers, the cluster can tolerate 1 controller failure; with 5 controllers, the cluster can tolerate 2 controller failures.</p>
    <li>For redundancy, a Kafka cluster should use 3 or 5 controllers, depending on factors like cost and the number of concurrent failures your system should withstand without availability impact. In the rare case of a partial network failure it is possible for the cluster metadata quorum to become unavailable. This limitation will be addressed in a future release of Kafka.</li>
```

**How to spot**
When reviewing docs/ops.html code, look for race condition / missing synchronization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17913 fix kraft controller count recommendations (#17657).

**Test**
No test added.
