# Bug 1191: Comparator Contract Violated in Dynamic Snitch Sorter Due to Concurrently Changing Scores (2.1 Branch)

**Commit**: 9d44186ee3 · **Ticket**: CASSANDRA-9519
**Subsystem**: src/java/org/apache/cassandra/locator/DynamicEndpointSnitch.java
**Category**: Race condition / missing synchronization

---

## Root Cause

`sortByProximityWithScore` on the 2.1 branch delegated to `super.sortByProximity`, which called `compareEndpoints` repeatedly during sorting. The scores map is replaced atomically via copy-on-write on each update cycle, but `this.scores` could be reassigned between individual comparisons within a single `Collections.sort()` call. Java's TimSort requires a comparator to produce consistent results for the duration of a sort; if the scores reference flips mid-sort, two comparisons of the same pair of endpoints can return opposite signs, violating the contract and causing `IllegalArgumentException` or silently wrong orderings. Snapshotting `this.scores` into a local variable once before sorting begins ensures all comparisons within that sort use the same immutable scores map.

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

Whenever a `Comparator` is constructed inline and closes over instance state that can be updated by another thread, verify that the shared reference is snapshotted into a `final` local before the sort begins. A concurrent write to the shared field after the snapshot is harmless because it updates the reference, not the map object the snapshot points to.

---

## Test

`DynamicEndpointSnitchTest.testConcurrency` added: a background thread continuously feeds latency timings while the main thread sorts a large host list, reliably reproducing the comparator contract violation.
