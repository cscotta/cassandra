# Bug 1338: Bloom Filter and SSTableReader Reference Leaked on Writer Close and Validate Failure (2nd Branch)

**Commit**: c14a7d51e6 · **Ticket**: CASSANDRA-8748
**Subsystem**: src/java/org/apache/cassandra/io/sstable/SSTableWriter.java, SSTableReader.java, SSTableSimple*Writer
**Category**: Resource leak

---

## Root Cause

Same two leaks as CASSANDRA-8748 applied to a second release branch. `SSTableWriter.close()` never closed the off-heap bloom filter (`iwriter.bf`), and `SSTableReader.validate()` threw `IllegalStateException` without first calling `releaseReference()`, leaking the newly opened reader's reference count and its associated resources. The fix adds a `close(boolean closeBf)` overload and ensures the bloom filter is released when the writer's lifecycle ends, and releases the reference in `validate()` before rethrowing.

### Before / After

```java
// BEFORE — SSTableWriter.close(), bloom filter never closed
public Pair<Descriptor, SSTableMetadata> close() {
    iwriter.close();
    dataFile.close();
    ...
}
```

```java
// AFTER — bloom filter explicitly closed by callers that own the writer
public Pair<Descriptor, SSTableMetadata> close(boolean closeBf) {
    iwriter.close();
    if (closeBf)
        iwriter.bf.close();
    dataFile.close();
    ...
}
```

```java
// BEFORE — reference leaked on validation failure
if (this.first.compareTo(this.last) > 0)
    throw new IllegalStateException(...);

// AFTER
if (this.first.compareTo(this.last) > 0) {
    releaseReference();
    throw new IllegalStateException(...);
}
```

---

## How to Spot

Review every exit path from a constructor or init method on a ref-counted resource; any exception thrown before all resources are properly handed off must be preceded by a release. When adding a boolean parameter to a `close()` method, ensure the default overload delegates correctly and that every call site is audited for which semantics it requires.

---

## Test

No test added.
