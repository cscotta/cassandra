# Bug 2545: Stress COUNTER_GET reads from wrong column family name

**Commit**: 556af02958 · **Ticket**: CASSANDRA-3301

## Detection Rule
When a stress/benchmark operation hardcodes a column family name that does not match the actual column family written during the write phase, reads will always miss and the benchmark produces incorrect results.

## Code Smell
A string literal column family name in a `ColumnParent` constructor differs from the name used during the corresponding write operation (e.g., `"CounterSuper1"` vs. `"SuperCounter1"`).

## Tags
- scope: cassandra
- category: wrong-constant
- code-pattern: hardcoded-wrong-table-name
- severity: silent-wrong-result
- subsystem: tools-clients

## Root Cause
`CounterGetter` constructed a `ColumnParent` with `"CounterSuper1"` but the write phase created data in `"SuperCounter1"`. All counter GET operations silently read from a non-existent column family, making COUNTER_GET benchmarks useless.

## Example
```java
// BEFORE
ColumnParent parent = new ColumnParent("CounterSuper1")
    .setSuper_column(superColumn.getBytes());
```
```java
// AFTER
ColumnParent parent = new ColumnParent("SuperCounter1")
    .setSuper_column(superColumn.getBytes());
```
