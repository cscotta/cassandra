# Bug 0294: IndexSummaryManager Resize Interval Changes Not Persisted to DatabaseDescriptor

**Commit**: 860ec627a1 · **Ticket**: CASSANDRA-17735
**Subsystem**: io/sstable / index-summary / config
**Category**: State not cleaned up

---

## Root Cause

`IndexSummaryManager` stored the resize interval in its own private field `resizeIntervalInMinutes`, initialised to `0`. The `setResizeIntervalInMinutes()` JMX method updated that private field, but `getResizeIntervalInMinutes()` also read the private field, meaning neither the Settings virtual table nor any caller going through `DatabaseDescriptor` ever saw the updated value. Additionally, the initial value of `0` was incorrect: the true startup value in `Config` was `60`, so the manager used `0` minutes as the rescheduling baseline until the first JMX call overwrote it. Index summary capacity had an analogous problem where `conf.index_summary_capacity_in_mb` was never written back with the computed default.

### Before / After

```java
// BEFORE - private shadow field, initial value wrong
private int resizeIntervalInMinutes = 0;

public int getResizeIntervalInMinutes()
{
    return resizeIntervalInMinutes;
}

public void setResizeIntervalInMinutes(int resizeIntervalInMinutes)
{
    int oldInterval = this.resizeIntervalInMinutes;
    this.resizeIntervalInMinutes = resizeIntervalInMinutes;
    // ...
    if (this.resizeIntervalInMinutes < 0)
```

```java
// AFTER - delegates entirely to DatabaseDescriptor
public int getResizeIntervalInMinutes()
{
    return DatabaseDescriptor.getIndexSummaryResizeIntervalInMinutes();
}

public void setResizeIntervalInMinutes(int resizeIntervalInMinutes)
{
    int oldInterval = getResizeIntervalInMinutes();
    DatabaseDescriptor.setIndexSummaryResizeIntervalInMinutes(resizeIntervalInMinutes);
    // ...
    if (resizeIntervalInMinutes < 0)
```

---

## How to Spot

Private mutable fields inside a singleton service that duplicate a value also stored in `Config` create divergence the moment either side is updated independently. The code smell is: a JMX bean that has both a `getX()` returning `this.x` and a `setX()` that writes `this.x` but does not call `DatabaseDescriptor.setX()`. Always delegate to `DatabaseDescriptor` and remove the shadow field to ensure a single source of truth visible to the Settings virtual table, JMX clients, and internal callers alike.

---

## Test

`IndexSummaryManagerTest` is extended to assert that after each `setResizeIntervalInMinutes()` call, `DatabaseDescriptor.getIndexSummaryResizeIntervalInMinutes()` returns the same value that `manager.getResizeIntervalInMinutes()` returns.
