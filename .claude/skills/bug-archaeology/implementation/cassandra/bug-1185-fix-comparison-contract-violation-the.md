# Bug 1185: Comparator Contract Violated in Dynamic Snitch Sorter Due to Concurrently Changing Scores

**Commit**: a9b9e627b0 · **Ticket**: CASSANDRA-9519
**Subsystem**: src/java/org/apache/cassandra/locator/DynamicEndpointSnitch.java
**Category**: Race condition / missing synchronization

---

## Root Cause

`sortByProximityWithScore` delegated to `super.sortByProximity`, which internally called `compareEndpoints` on the instance. The scores map is replaced atomically on each update cycle (copy-on-write), but the reference `this.scores` could change between two comparisons during a single `Collections.sort()` call. Java's sort algorithm requires a comparator to be consistent for the duration of the sort: if `compare(a, b) < 0` at one point and `compare(a, b) > 0` a moment later for identical arguments, the sort contract is violated, leading to `IllegalArgumentException` (TimSort) or silently wrong orderings. Because the scores reference was read fresh on each comparison call rather than once before sorting began, any concurrent score update during a sort triggered this inconsistency.

### Before / After

```java
// BEFORE
private void sortByProximityWithScore(final InetAddress address, List<InetAddress> addresses)
{
    super.sortByProximity(address, addresses);
}
```

```java
// AFTER
private void sortByProximityWithScore(final InetAddress address, List<InetAddress> addresses)
{
    final HashMap<InetAddress, Double> scores = this.scores; // snapshot once
    Collections.sort(addresses, new Comparator<InetAddress>()
    {
        public int compare(InetAddress a1, InetAddress a2)
        {
            return compareEndpoints(address, a1, a2, scores);
        }
    });
}
```

---

## How to Spot

Whenever a comparator reads shared mutable state on each invocation rather than capturing a snapshot before the sort begins, a concurrent writer can invalidate the consistency contract mid-sort. Review all `Comparator` implementations that access instance fields or volatile references: if the field can change between two calls, snapshot it into a local variable before passing the comparator to any sort routine.

---

## Test

`DynamicEndpointSnitchTest.testConcurrency` added: spawns a background thread that continuously calls `receiveTiming` on random hosts while the main thread sorts a 655 360-host list repeatedly, reliably triggering the old violation.
