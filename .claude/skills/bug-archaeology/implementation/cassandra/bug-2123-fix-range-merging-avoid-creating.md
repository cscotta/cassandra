# Bug 2123: Range Merging Creates Wrapping Ranges That CFS.getRangeSlice Cannot Handle

**Commit**: 19972bdd91 · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/service/StorageProxy.java
**Category**: Logic error in condition

---

## Root Cause

`StorageProxy` merges adjacent token ranges when they share the same live replica set, reducing the number of round-trips. However, when `range.right` is the minimum token (i.e., the range ends at the ring's wrap-around point), continuing to merge would produce a wrapping range whose right endpoint is past the minimum token. `CFS.getRangeSlice` does not know how to handle such wrapping ranges, causing incorrect or missing results. The fix adds an explicit break when the current range's right boundary is the minimum token, stopping the merge loop before a wrapping range can be constructed.

### Before / After

```java
// BEFORE
// (no guard; merge loop continued even when range.right was the min token)
List<InetAddress> merged = intersection(liveEndpoints, nextEndpoints);
```

```java
// AFTER
if (range.right.isMinimum())
    break;

List<InetAddress> merged = intersection(liveEndpoints, nextEndpoints);
```

---

## How to Spot

Whenever range-merging logic is applied to ring ranges, check whether a wrap-around sentinel value (min token) requires a special termination condition. Ranges that reach the ring's minimum token are logically complete and should not be extended further.

---

## Test

No test added.
