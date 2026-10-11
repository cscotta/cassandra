# Bug 2075: NPE in CompactionManager When markAllCompacting Returns Null

**Commit**: c8c2044e40 · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/db/compaction/CompactionManager.java
**Category**: Missing null / bounds check

---

## Root Cause

`performAllSSTableOperation` called `cfs.markAllCompacting()` and immediately passed the result to a `Callable` without checking whether it was `null`. `markAllCompacting()` returns `null` when there are no SSTables to compact (a no-op case), causing a `NullPointerException` when the callable later tried to iterate the collection.

### Before / After

```java
// BEFORE
final Collection<SSTableReader> sstables = cfs.markAllCompacting();
Callable<Object> runnable = new Callable<Object>()
{
    public Object call() throws IOException
```

```java
// AFTER
final Collection<SSTableReader> sstables = cfs.markAllCompacting();
if (sstables == null)
    return;

Callable<Object> runnable = new Callable<Object>()
{
    public Object call() throws IOException
```

---

## How to Spot

Whenever a method that "marks" or "collects" a set of resources returns a nullable collection, add a null-guard before passing the result into any lambda or callable that assumes a non-null collection. Look for patterns where the return value of a mark/acquire method is captured and immediately closed over without an intermediate null check.

---

## Test

No test added.
