**Commit**: e2060204fe · **Ticket**: KAFKA-15302
**Subsystem**: docs/streams
**Category**: Logic error in condition

**Root cause**
Reviewers: Matthias J. Sax <matthias@confluent.io>

**Before/After**
```java
// Before
// (no removals)

// After
      <p>To avoid reading stale data, you can <code class="docutils literal"><span class="pre">flush()</span></code> the store before creating the iterator.
        Note, that flushing too often can lead to performance degration if RocksDB is used, so we advice to avoid flushing manually in general.</p>
```

**How to spot**
When reviewing docs/streams code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-15302: stale value returned when using store.all() with key deletion [docs] (#15495).

**Test**
No test added.
