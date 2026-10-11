# Bug 2045: SemanticVersion.isSupportedBy Incorrectly Compares Minor/Patch Components Independently

**Commit**: 455a0fccfb · **Ticket**: CASSANDRA-5496
**Subsystem**: src/java/org/apache/cassandra/utils/SemanticVersion.java
**Category**: Logic error in condition

---

## Root Cause

`SemanticVersion.isSupportedBy` checked `minor <= version.minor && patch <= version.patch` as independent conditions. This produced wrong results when the minor version of the candidate was greater than the support version: e.g., `3.1.0.isSupportedBy(3.0.1)` returned `true` because `patch(0) <= patch(1)` passed even though `minor(1) > minor(0)`. The semantics require a lexicographic comparison of the full version tuple, which is exactly what the existing `compareTo` method provides.

### Before / After

```java
// BEFORE
return major == version.major && minor <= version.minor && patch <= version.patch;
```

```java
// AFTER
return major == version.major && this.compareTo(version) <= 0;
```

---

## How to Spot

When two numeric fields are combined with independent `<=` comparisons to express a "version at most X" relationship, verify that the comparisons form a proper lexicographic order rather than allowing impossible combinations to pass (e.g., a higher minor with a lower patch satisfying both inequalities independently).

---

## Test

`SemanticVersionTest.testIsSupportedBy` is extended with two new cases: `3.1.0.isSupportedBy(3.0.1)` must return `false`, and `3.0.1.isSupportedBy(3.1.0)` must return `true`, directly covering the broken scenario.
