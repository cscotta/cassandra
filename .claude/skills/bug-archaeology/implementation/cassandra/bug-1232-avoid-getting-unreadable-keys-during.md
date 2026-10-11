# Bug 1232: Avoid Getting Unreadable Keys During Anticompaction

**Commit**: eaeabffc8e · **Ticket**: CASSANDRA-9508
**Subsystem**: src/java/org/apache/cassandra/io/sstable/SSTableRewriter.java
**Category**: Race condition / missing synchronization

---

## Root Cause

During anticompaction, `SSTableRewriter` was configured with preemptive early-opening of SSTables enabled (the default). When an SSTable is preemptively opened partway through writing, it becomes visible in the `DataTracker` before all keys are written, allowing read threads to attempt to access keys that have not yet been flushed to disk. The anticompaction path should never open SSTables early because it produces two output writers simultaneously (repaired and unrepaired), making partial-open visibility particularly dangerous.

### Before / After

```java
// BEFORE
SSTableRewriter repairedSSTableWriter = new SSTableRewriter(cfs, sstableAsSet, sstable.maxDataAge, false);
SSTableRewriter unRepairedSSTableWriter = new SSTableRewriter(cfs, sstableAsSet, sstable.maxDataAge, false);
// preemptive open interval computed from static field, always enabled if configured
```

```java
// AFTER
SSTableRewriter repairedSSTableWriter = new SSTableRewriter(cfs, sstableAsSet, sstable.maxDataAge, false, false);
SSTableRewriter unRepairedSSTableWriter = new SSTableRewriter(cfs, sstableAsSet, sstable.maxDataAge, false, false);
// new boolean param `shouldOpenEarly=false` disables preemptive open for anticompaction
```

---

## How to Spot

Look for `SSTableRewriter` (or similar write-then-register patterns) being constructed without explicitly opting out of early visibility features. Any code path that writes an SSTable in a context where partial data must not be readable—such as anticompaction, bulk loading, or offline rebuilds—should pass `shouldOpenEarly=false` or equivalent. Constructors whose defaults enable early-open are a latent hazard whenever a new call site is added.

---

## Test

`SSTableRewriterTest` was extended with additional anticompaction scenarios covering the early-open path.
