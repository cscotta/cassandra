# Bug 0209: minRowsRequested not forwarded to REST server during scan planning

**Commit**: c9f6c8423be7cbf52c9cf678896deca478b3b6d1 · **Ticket**: #15661

## Detection Rule

When a client builds a request object from a scan context, verify that every field present in the scan context (e.g. `minRowsRequested`, `statsFields`, `caseSensitive`) has a corresponding mapping into the request object; missing mappings silently drop user-specified query hints.

## Code Smell

A `PlanTableScanRequest.Builder` chain that sets several scan-context fields (`withSelect`, `withFilter`, `withCaseSensitive`, `withStatsFields`) but omits `withMinRowsRequested`, while the corresponding server-side handler and `planFilesFor` method have parameters that accept the value if it were provided.

## Tags

- scope: project-specific
- category: silent-skip
- code-pattern: field-not-mapped-to-request
- severity: performance
- subsystem: core/rest

## Root Cause

`RESTTableScan` builds the `PlanTableScanRequest` by copying fields from the scan context, but the `minRowsRequested` field was never included in the builder chain. As a result, the server always received `null` for that field and planned the full scan, returning far more scan tasks than necessary for queries with a `LIMIT` clause. The server-side `planFilesFor` method already supported `minRowsRequested` as a truncation hint once it was plumbed through.

## Example

```java
// BEFORE — minRowsRequested silently dropped from request
PlanTableScanRequest.Builder builder = ImmutablePlanTableScanRequest.builder()
    .withSelect(selectedColumns)
    .withFilter(filter())
    .withCaseSensitive(isCaseSensitive())
    .withStatsFields(statsFields);
```

```java
// AFTER — minRowsRequested forwarded to server
PlanTableScanRequest.Builder builder = ImmutablePlanTableScanRequest.builder()
    .withSelect(selectedColumns)
    .withFilter(filter())
    .withCaseSensitive(isCaseSensitive())
    .withStatsFields(statsFields)
    .withMinRowsRequested(context().minRowsRequested());
```
