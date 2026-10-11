# Bug 2512: SSTableMetadata row-size and column-count histograms swapped in Collector

**Commit**: 50ee32f685 · **Ticket**: CASSANDRA-3451

## Detection Rule
When two histogram factory methods exist with different bucket counts for different metrics, verify that each call site uses the correct factory by cross-referencing the field name with the factory name.

## Code Smell
`this.estimatedRowSize = defaultColumnHistogram()` and `this.estimatedColumnCount = defaultRowHistogram()` — the field names and factory names are swapped, so row-size is tracked with column-count buckets (too small at 114) and column-count is tracked with row-size buckets (over-sized at 150).

## Tags
- scope: cassandra
- category: wrong-constant
- code-pattern: swapped-histogram-factories
- severity: silent-wrong-result
- subsystem: storage-engine

## Root Cause
Two helper methods, `defaultRowHistogram()` (150 buckets, tracking up to ~1.5 PB) and `defaultColumnHistogram()` (114 buckets, tracking up to ~2B columns), were introduced. The `Collector` inner class constructor then assigned them to the wrong fields: `estimatedRowSize` got the 114-bucket histogram (capping row-size estimates far too low) and `estimatedColumnCount` got the 150-bucket one (wasting memory). Compaction and leveled compaction both use `estimatedRowSize` to calculate thresholds, so incorrect histograms could cause premature or missed compaction triggers.

## Example
```java
// BEFORE (swapped)
private Collector() {
    this.estimatedRowSize = defaultColumnHistogram();    // 114 buckets — wrong
    this.estimatedColumnCount = defaultRowHistogram();   // 150 buckets — wrong
}
```
```java
// AFTER
private Collector() {
    this.estimatedRowSize = defaultRowSizeHistogram();   // 150 buckets
    this.estimatedColumnCount = defaultColumnCountHistogram(); // 114 buckets
}
```
