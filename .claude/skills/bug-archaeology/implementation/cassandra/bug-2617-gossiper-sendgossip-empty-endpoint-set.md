# Bug 2617: Gossiper sendGossip panics with index-out-of-range on empty endpoint set

**Commit**: 931c2f1208 · **Ticket**: n/a

## Detection Rule
When picking a random element from a list, guard against an empty list before calling `random.nextInt(size)` or accessing by index.

## Code Smell
`List<InetAddress> liveEndpoints = new ArrayList<>(epSet); int index = (size == 1) ? 0 : random.nextInt(size);` without a prior `if (size < 1) return` will call `random.nextInt(0)` which throws `IllegalArgumentException`.

## Tags
- scope: cassandra
- category: missing-null-check
- code-pattern: random-nextint-zero-size
- severity: crash
- subsystem: gossip

## Root Cause
`Gossiper.sendGossip()` computed `size = epSet.size()` and then either used `0` or `random.nextInt(size)` as the list index. When `size` was 0, `random.nextInt(0)` threw `IllegalArgumentException`. This could occur transiently when the unreachable-endpoints set was observed as empty between the size check and the random selection. The fix adds an early `if (size < 1) return false` guard.

## Example
```java
// BEFORE
int size = epSet.size();
List<InetAddress> liveEndpoints = new ArrayList<InetAddress>(epSet);
int index = (size == 1) ? 0 : random.nextInt(size); // crashes when size==0
```
```java
// AFTER
int size = epSet.size();
if (size < 1)
    return false;
List<InetAddress> liveEndpoints = new ArrayList<InetAddress>(epSet);
int index = (size == 1) ? 0 : random.nextInt(size);
```
