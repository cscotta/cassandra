# Bug 0031: Race Condition in AccordService MetadataChangeListener and Wrong Histogram getRecentValues

**Commit**: e4684a11ec · **Ticket**: CASSANDRA-21076
**Subsystem**: service/accord/AccordService, metrics/OnDemandHistogram, metrics/ShardedHistogram
**Category**: Race condition / missing synchronization

---

## Root Cause

`AccordService.MetadataChangeListener` used a mutable `List<ClusterMetadata>` inside a `synchronized` `PreInitStateCollector`, but swapped the collector reference via `AtomicReference.getAndSet` non-atomically: between reading the old collector and installing the new `ChangeListener`, a concurrent `notifyPostCommit` call could still write into the old collector while the caller was draining its items, causing epochs to be silently dropped or processed twice. The fix replaces the mutable list with an immutable `Collector` that builds a new array on each update, making the CAS loop the only synchronisation point.

A second related bug: `OnDemandHistogram.isCumulative()` was not overridden and always inherited the base-class `true` default from `OverrideHistogram`. Non-cumulative histograms (e.g. latency snapshots) then had `getRecentValues` subtract prior values from a non-cumulative counter, producing wrong (and potentially negative) metric readings.

### Before / After

```java
// BEFORE – mutable list under synchronized, swapped via getAndSet
static class PreInitStateCollector implements ChangeListener {
    private final List<ClusterMetadata> items = new ArrayList<>(4);
    public synchronized void notifyPostCommit(...) { items.add(next); }
}
private final AtomicReference<ChangeListener> collector = new AtomicReference<>(new PreInitStateCollector());
```

```java
// AFTER – immutable Collector swapped via CAS loop
static final class Collector implements Sink {
    final ClusterMetadata[] saved;
    public Sink update(ClusterMetadata next) {
        ClusterMetadata[] newSaved = Arrays.copyOf(saved, saved.length + 1);
        newSaved[saved.length] = next;
        return new Collector(newSaved);
    }
}
private final AtomicReference<Sink> sink = new AtomicReference<>(Collector.EMPTY);
```

```java
// BEFORE – isCumulative not overridden; always returns true
protected OnDemandHistogram(Supplier<LogLinearSnapshot> snapshot) { ... }
```

```java
// AFTER
protected OnDemandHistogram(Supplier<LogLinearSnapshot> snapshot, boolean isCumulative) {
    this.isCumulative = isCumulative;
}
@Override public boolean isCumulative() { return isCumulative; }
```

---

## How to Spot

When draining a shared collector after atomically replacing it with a new one, verify that no window exists between the swap and the drain where concurrent writers can still append to the old instance. Immutable value objects combined with a CAS loop eliminate this window entirely. For histogram subclasses, always override `isCumulative()` explicitly rather than relying on an inherited default, especially when the parent class defaults to `true`.

---

## Test

No dedicated unit test for the race; the fix is structural. The `AccordSimpleFastPathTest` and `EpochSyncTest` in-JVM dtests exercise the initialization path.
