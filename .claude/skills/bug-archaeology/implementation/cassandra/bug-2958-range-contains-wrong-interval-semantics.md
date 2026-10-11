# Bug 2958: Range.contains() uses wrong interval semantics — [left, right) instead of (left, right]

**Commit**: b1c5077f44 · **Ticket**: CASSANDRA-495

## Detection Rule
When a `Range.contains(token)` method uses `>= left` for the lower bound check, verify whether the ring convention is left-exclusive (left, right] — if so, the boundary token `left` itself should not be included.

## Code Smell
`Range.contains()` returned `true` when `bi.compareTo(left_) >= 0` for normal ranges and wrap-arounds, and `isWrapAround()` used `left_.compareTo(right_) > 0` (strict), all consistent with `[left, right)` semantics that contradicted the rest of Cassandra's ring convention.

## Tags
- scope: cassandra
- category: wrong-constant
- code-pattern: inclusive-exclusive-boundary-inversion
- severity: silent-wrong-result
- subsystem: storage-engine

## Root Cause
The DHT ring in Cassandra assigns responsibility for tokens in the range `(predecessor_token, my_token]` — left-exclusive, right-inclusive. The `Range.contains()` method had it backwards: it included the left endpoint and excluded the right endpoint (`[left, right)`). This means a token exactly equal to `left` would incorrectly be reported as contained, while a token equal to `right` would be missed. This affected range queries, repair, bootstrapping range calculations, and streaming. `isWrapAround()` was also changed from `> 0` to `>= 0` to handle the equal-endpoints case (full ring wrap).

## Example
```java
// BEFORE: [left, right) semantics — left included, right excluded
if (bi.compareTo(left_) >= 0) return true;  // should be >
else return right_.compareTo(bi) > 0;       // should be >=
```
```java
// AFTER: (left, right] semantics — left excluded, right included
if (bi.compareTo(left_) > 0) return true;
else return right_.compareTo(bi) >= 0;
```
