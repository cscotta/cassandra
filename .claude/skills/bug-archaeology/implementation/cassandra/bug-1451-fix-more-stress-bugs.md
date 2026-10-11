# Bug 1451: Stress tool batch subList bound and silent ClassCastException

**Commit**: a8052d7c81 · **Ticket**: n/a
**Subsystem**: tools/stress/src/org/apache/cassandra/stress/
**Category**: Logic error in condition

---

## Root Cause

Two independent bugs in the stress tool. First, in `SchemaInsert.java` the batch-chunking loop used `Math.min(stmts.size(), j + 65535)` as the upper bound for `subList()`, but `stmts.size()` is an absolute count while `j` is the running offset; once `j > 0` the effective upper bound shrinks below the correct value, causing later chunks to be silently truncated. The correct expression is `Math.min(j + stmts.size(), j + 65535)` — though in practice this simplifies to `Math.min(stmts.size(), j + 65535)` only when `j == 0`. Second, in `Partition.java` the SORTED generator path incorrectly guarded `queue.add()` with `i == 0 || tosort.get(i-1).compareTo(i) < 0`, comparing a generated value to an *integer index* rather than to the previous element, dropping valid entries; the guard was simply removed, letting all sorted values through. A missing `else` branch for a non-`Comparable` generator was also added to surface failures early.

### Before / After

```java
// BEFORE — upper bound ignores the current offset j
List<BoundStatement> substmts = stmts.subList(j, Math.min(stmts.size(), j + 65535));
```

```java
// AFTER — upper bound is relative to j
List<BoundStatement> substmts = stmts.subList(j, Math.min(j + stmts.size(), j + 65535));
```

```java
// BEFORE — erroneous guard drops sorted values
if (i == 0 || tosort.get(i - 1).compareTo(i) < 0)
    queue.add(tosort.get(i));
```

```java
// AFTER — all sorted values enqueued unconditionally
queue.add(tosort.get(i));
```

---

## How to Spot

In sliding-window or chunked-iteration patterns, double-check every `subList`/`slice` argument: the end index should always be expressed relative to the same origin as the start index. A mismatch between an absolute size and a relative offset is easy to introduce and difficult to notice because the first iteration (j=0) is always correct. Similarly, any `compareTo(i)` where `i` is a loop counter rather than a typed element of the same collection is almost certainly a type confusion bug.

---

## Test

No test added.
