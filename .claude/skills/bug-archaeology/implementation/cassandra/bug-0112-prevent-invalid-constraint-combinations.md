# Bug 0112: No Validation of Contradictory or Redundant Column Constraint Combinations

**Commit**: 980047657d · **Ticket**: CASSANDRA-20330
**Subsystem**: `src/java/org/apache/cassandra/cql3/constraints`
**Category**: Logic error in condition

---

## Root Cause

The column-constraint subsystem accepted `CREATE TABLE` statements with logically unsatisfiable or structurally redundant constraint pairs such as `CHECK (LEN(v) > 5 AND LEN(v) >= 5)`, `CHECK (v > 10 AND v < 5)`, or two identical `NOT EQUAL` predicates on the same column. No satisfiability checker existed; constraints were stored as-is and could only be detected as wrong at write time (or never, for the impossible cases). The fix adds `AbstractFunctionSatisfiabilityChecker` and a per-`ConstraintType` checker array that is invoked at DDL time to reject contradictory bound combinations, mixed GT/GTE or LT/LTE pairs, standalone EQ combined with another predicate, and exact duplicate NEQ terms.

### Before / After

```java
// BEFORE – ConstraintType carried only a serializer, no satisfiability logic
ConstraintType(MetadataSerializer<?> serializer) {
    this.serializer = serializer;
}
```

```java
// AFTER – each ConstraintType also carries one or more SatisfiabilityChecker instances
ConstraintType(MetadataSerializer<?> serializer, SatisfiabilityChecker[] satisfiabilityCheckers) {
    this.serializer = serializer;
    this.satisfiabilityCheckers = satisfiabilityCheckers;
}
```

---

## How to Spot

When a system accepts compound declarative predicates at definition time but only evaluates them individually at enforcement time, impossible or redundant combinations can slip through. During code review, ask: is there a cross-constraint satisfiability check at the point where all constraints for a column are assembled? If constraints are stored in a list and each is only validated in isolation, any pair whose conjunction is a tautology (e.g., `v > 5 AND v >= 5`) or contradiction (e.g., `v > 10 AND v < 5`) will be silently accepted.

---

## Test

`ConstraintsSatisfiabilityTest` verifies that unsatisfiable and redundant scalar and function constraint pairs throw `InvalidConstraintDefinitionException` at `CREATE TABLE` time.
