# Bug 1702: Fix NPE in MeteredFlusher

**Commit**: 535c56fb21 · **Ticket**: CASSANDRA-6820
**Subsystem**: src/java/org/apache/cassandra/db/MeteredFlusher.java
**Category**: Race condition / missing synchronization

---

## Root Cause

`MeteredFlusher` read the volatile field `Memtable.activelyMeasuring` twice in a single expression: once in the null check and once to dereference it. Between those two reads, another thread could set the field to `null`, making the null check meaningless and causing a `NullPointerException` on the dereference. The fix reads the volatile field exactly once into a local variable and uses that local for both the null check and the subsequent call, eliminating the TOCTOU window.

### Before / After

```java
// BEFORE — double-read of volatile: null check and dereference are not atomic
long flushingBytes = Memtable.activelyMeasuring == null
                   ? 0
                   : Memtable.activelyMeasuring.getMemtableThreadSafe().getLiveSize();
```

```java
// AFTER — single read into local variable
ColumnFamilyStore measuredCfs = Memtable.activelyMeasuring;
long flushingBytes = measuredCfs == null ? 0 : measuredCfs.getMemtableThreadSafe().getLiveSize();
```

---

## How to Spot

Any pattern of the form `if (volatileField != null) { volatileField.method() }` or its ternary equivalent is a check-then-act race on a volatile. Always capture a volatile reference in a local variable before branching on it.

---

## Test

No test added.
