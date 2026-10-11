# Bug 1337: Bloom Filter and SSTableReader Reference Leaked on Writer Close and Validate Failure

**Commit**: b1825e6f84 · **Ticket**: CASSANDRA-8748
**Subsystem**: src/java/org/apache/cassandra/io/sstable/SSTableWriter.java, SSTableReader.java, SSTableSimple*Writer
**Category**: Resource leak

---

## Root Cause

Two independent leaks existed in the SSTable write path. First, `SSTableWriter.close()` closed the index writer's structures but never closed the bloom filter (`iwriter.bf`), which holds off-heap memory; callers such as `SSTableSimpleWriter` and `SSTableSimpleUnsortedWriter` were left with no way to release that memory. Second, `SSTableReader.validate()` threw an `IllegalStateException` when `first > last` but did so without first calling `releaseReference()`, leaking the reference count and the associated resources of the newly opened reader.

### Before / After

```java
// BEFORE — SSTableWriter.close(), bloom filter never closed
public Pair<Descriptor, SSTableMetadata> close() {
    iwriter.close();
    // iwriter.bf is never closed
    dataFile.close();
    ...
}
```

```java
// AFTER — callers pass closeBf=true; writer releases filter when done
public Pair<Descriptor, SSTableMetadata> close(boolean closeBf) {
    iwriter.close();
    if (closeBf)
        iwriter.bf.close();
    dataFile.close();
    ...
}
```

```java
// BEFORE — validate() throws without releasing the reader reference
private void validate() {
    if (this.first.compareTo(this.last) > 0)
        throw new IllegalStateException(...);
}

// AFTER — reference released before throwing
private void validate() {
    if (this.first.compareTo(this.last) > 0) {
        releaseReference();
        throw new IllegalStateException(...);
    }
}
```

---

## How to Spot

Any time an exception is thrown from a constructor or an initialisation method on a ref-counted object, check that all previously acquired references are released before the throw. Similarly, when a `close()` method is introduced with an overloaded boolean parameter, audit all call sites: the default (false) overload must not silently skip cleanup that some callers need.

---

## Test

No test added.
