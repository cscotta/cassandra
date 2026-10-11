# Bug 2953: Local endpoint detection broken by Arrays.asList wrapping

**Commit**: ed808be3d3 · **Ticket**: CASSANDRA-512

## Detection Rule
When checking list membership with `contains()`, verify whether the list was created with `Arrays.asList()` on an already-List type — if `getNaturalEndpoints` returns a `List<InetAddress>`, wrapping it in `Arrays.asList()` creates a `List<List<InetAddress>>` whose `contains(InetAddress)` always returns false.

## Code Smell
`Arrays.asList(endpoints).contains(FBUtilities.getLocalAddress())` where `endpoints` is already a `List<InetAddress>` — `Arrays.asList` of a single `List` argument produces a one-element list containing that list, so `contains(InetAddress)` always returns false.

## Tags
- scope: cassandra
- category: wrong-filter-result
- code-pattern: arrays-asList-double-wrap
- severity: silent-wrong-result
- subsystem: cql-query

## Root Cause
A regression from a prior refactor changed `endpoints` from an array to a `List<InetAddress>`. The call `Arrays.asList(endpoints)` that previously converted an array to a list now wraps the list in another list, making `contains(localAddress)` always return false. This caused all reads to be routed to remote nodes even when the local node was a natural endpoint, bypassing local short-circuit reads entirely. The fix is simply `endpoints.contains(FBUtilities.getLocalAddress())`.

## Example
```java
// BEFORE: double-wraps the list — contains() always returns false
boolean foundLocal = Arrays.asList(endpoints).contains(FBUtilities.getLocalAddress());
```
```java
// AFTER: direct contains() on the existing List
boolean foundLocal = endpoints.contains(FBUtilities.getLocalAddress());
```
