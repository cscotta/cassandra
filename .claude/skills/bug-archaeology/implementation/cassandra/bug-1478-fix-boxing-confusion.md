# Bug 1478: Unnecessary Auto-Boxing via valueOf() / parseBoolean() Confusion

**Commit**: 5cc7a11ab8 · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/db/Memtable.java, dht/RangeStreamer.java, hadoop/cql3/CqlConfigHelper.java, tools/StandaloneSplitter.java, stress/settings/OptionAnyProbabilities.java
**Category**: Wrong constant / default value

---

## Root Cause

Several sites used `Integer.valueOf(String)` and `Boolean.valueOf(String)` where `Integer.parseInt(String)` and `Boolean.parseBoolean(String)` (or vice-versa) were semantically more appropriate. `Integer.valueOf` returns a boxed `Integer` that must be unboxed to a primitive `int` via an implicit unbox, adding unnecessary autoboxing overhead and making intent unclear. Similarly, `Boolean.valueOf` returns a cached `Boolean` object instead of a primitive `boolean`. At the `static final` field initialization site in `Memtable`, this could also mask a `NullPointerException` if the unbox of a cached `null` were ever reached. The fix consistently uses the parse variants for primitive assignments and the valueOf variants where a boxed type is required.

### Before / After

```java
// BEFORE (Memtable.java)
private static final int ROW_OVERHEAD_HEAP_SIZE =
    estimateRowOverhead(Integer.valueOf(System.getProperty("cassandra.memtable_row_overhead_computation_step", "100000")));

// BEFORE (RangeStreamer.java)
public static final boolean useStrictConsistency =
    Boolean.valueOf(System.getProperty("cassandra.consistent.rangemovement","true"));
```

```java
// AFTER (Memtable.java)
private static final int ROW_OVERHEAD_HEAP_SIZE =
    estimateRowOverhead(Integer.parseInt(System.getProperty("cassandra.memtable_row_overhead_computation_step", "100000")));

// AFTER (RangeStreamer.java)
public static final boolean useStrictConsistency =
    Boolean.parseBoolean(System.getProperty("cassandra.consistent.rangemovement","true"));
```

---

## How to Spot

Flag any assignment of `Integer.valueOf(String)` to a primitive `int` variable or parameter, and `Boolean.valueOf(String)` to a primitive `boolean`. Static analysis tools (FindBugs/SpotBugs rule `BX_UNBOXING_IMMEDIATELY_REBOXED` or similar) catch this pattern automatically.

---

## Test

No test added.
