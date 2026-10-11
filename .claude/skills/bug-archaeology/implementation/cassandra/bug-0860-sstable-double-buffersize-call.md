# Bug 0860: SSTable buffer size computed twice via nested optimizationStrategy.bufferSize calls

**Commit**: c4c9b05700 · **Ticket**: CASSANDRA-12292
**Subsystem**: SSTable I/O / FileHandle
**Category**: Wrong constant / default value

---

## Root Cause
The buffer-size for the index and data file handles was computed by calling `optimizationStrategy.bufferSize(x)` twice: once to produce the desired size and then again to "optimise" the result. Because `bufferSize` rounds its input up to the next power-of-two (or some strategy-defined value), applying it twice inflated the buffer far beyond the intended size. The fix removes the redundant outer call, passing the already-computed size directly to the `FileHandle` builder. This affected three call sites: `SSTableReader.openForBatch`, `BigTableWriter.openEarly`, and `BigTableWriter.finish`.

### Before / After
```java
// BEFORE  (SSTableReader.java)
sstable.ifile = ibuilder.bufferSize(sstable.optimizationStrategy.bufferSize(indexBufferSize)).complete();
sstable.dfile = dbuilder.bufferSize(sstable.optimizationStrategy.bufferSize(dataBufferSize)).complete();

// AFTER
sstable.ifile = ibuilder.bufferSize(indexBufferSize).complete();
sstable.dfile = dbuilder.bufferSize(dataBufferSize).complete();
```

---

## How to Spot
Watch for calls of the form `f(f(x))` where `f` is a sizing or rounding function. In particular, any helper that already normalises its output (rounds to power-of-two, aligns to page size, etc.) must not be called on a value that was produced by the same helper. Code reviewers should trace every `bufferSize` argument backwards to confirm the value was computed by strategy arithmetic, not by a prior `bufferSize` call.

---

## Test
No new test added. The bug was introduced by CASSANDRA-11580 and identified by inspection; the fix is verified by correctness argument rather than an added regression test.
