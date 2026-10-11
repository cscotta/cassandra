# Bug 2769: Repair Callback Uses Wrong Predicate to Check for Resolved Data

**Commit**: 74c464a36d · **Ticket**: n/a (see CASSANDRA-2069)

## Detection Rule
When a repair callback's `get()` must decide whether to call `resolve()`, check `resolver.getMessageCount() > 0` rather than `resolver.isDataPresent()`, which tests a different (potentially absent) condition.

## Code Smell
`resolver.isDataPresent() ? resolver.resolve() : null` returns `null` on timeout even when messages arrived, if `isDataPresent()` tests for data rather than message receipt.

## Tags
- scope: cassandra
- category: wrong-filter-result
- code-pattern: wrong-predicate-for-resolve
- severity: silent-wrong-result
- subsystem: repair

## Root Cause
`RepairCallback.get()` returned `null` when `isDataPresent()` was false, but `isDataPresent()` checked whether the resolver had actual row data — not whether any response messages had arrived. On timeout, messages may have arrived without data being present (e.g., empty-row responses), so the result was incorrectly dropped. The fix uses `getMessageCount() > 0` which correctly reflects whether any replies were received.

## Example
```java
// BEFORE
return resolver.isDataPresent() ? resolver.resolve() : null;
```
```java
// AFTER
return resolver.getMessageCount() > 0 ? resolver.resolve() : null;
```
