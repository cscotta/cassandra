**Commit**: 984777f0b9 · **Ticket**: KAFKA-17875
**Subsystem**: docs/ops.html
**Category**: Race condition / missing synchronization

**Root cause**
Reviewers: Mickael Maison <mickael.maison@gmail.com>
Co-authored-by: yungh <yungh@nvidia.com>

**Before/After**
```java
// Before
  <p>A Kafka admin will typically select 3 or 5 servers for this role, depending on factors like cost and the number of concurrent failures your system should withstand without availability impact. A majority of the controllers must be alive in order to maintain availability. With 3 controllers, the cluster can tolerate 1 controller failure; with 5 controllers, the cluster can tolerate 2 controller failures.</p>

// After
  <p>A Kafka admin will typically select 3 servers for this role, depending on factors like cost and the number of concurrent failures your system should withstand without availability impact. A majority of the controllers must be alive in order to maintain availability. With 3 controllers, the cluster can tolerate 1 controller failure; for configurations with more than 3 controllers, see <a href="#kraft_deployment">Deploying Considerations</a> for more details.</p>
```

**How to spot**
When reviewing docs/ops.html code, look for race condition / missing synchronization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17875: align kraft controller count recommendations (#17600).

**Test**
No test added.
