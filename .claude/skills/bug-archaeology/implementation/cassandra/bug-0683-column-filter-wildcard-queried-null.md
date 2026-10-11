# Bug 0683: ColumnFilter Wildcard Query Sets queried == null, Breaking queriedColumns()

**Commit**: 9359e1e977 · **Ticket**: CASSANDRA-13650
**Subsystem**: `src/java/org/apache/cassandra/db/filter/ColumnFilter.java`
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

`ColumnFilter` uses `queried == null` as a sentinel to mean "wildcard — all columns requested". The constructor stored the `queried` field correctly, but in the `fetchAllRegulars` branch it also assigned `this.fetched = this.queried = all` for the null case and `this.fetched = <statics-adjusted>; this.queried = queried` otherwise, making `fetched` a copy of `queried`. The flaw was in `queriedColumns()`, which returned the raw `queried` field without guarding against null, and in the constructor's `fetchAllRegulars` branch which over-eagerly set `queried` to `all` for the wildcard case, erasing the null sentinel. This caused wildcard queries — where `queried` should be null — to return a non-null but incorrect column set, breaking downstream logic that distinguished wildcard from explicit selection.

### Before / After

```java
// BEFORE – constructor fetchAllRegulars branch
if (queried == null) {
    this.fetched = this.queried = all;
} else {
    this.fetched = all.statics.isEmpty() ? all
                   : new RegularAndStaticColumns(queried.statics, all.regulars);
    this.queried = queried;
}
```

```java
// AFTER
this.fetched = (all.statics.isEmpty() || queried == null)
               ? all
               : new RegularAndStaticColumns(queried.statics, all.regulars);
// queried assigned once, outside both branches
this.queried = queried;
```

```java
// BEFORE – queriedColumns()
public RegularAndStaticColumns queriedColumns() { return queried; }

// AFTER
public RegularAndStaticColumns queriedColumns() {
    return queried == null ? fetched : queried;
}
```

---

## How to Spot

When a class uses null as a sentinel but has multiple assignment paths in the constructor, check that the sentinel is never overwritten and that every accessor that returns the sentinel-bearing field guards against the null case. The pattern "assign null to mean wildcard, then accidentally overwrite it with a real value in one branch" is easy to miss during refactoring.

---

## Test

New `testColumnFilterConstruction` test verifies that `ColumnFilter.all()` leaves `queried` null and that `queriedColumns()` returns the full column set; serialization round-trip tests cover all combinations of filter types and messaging protocol versions.
