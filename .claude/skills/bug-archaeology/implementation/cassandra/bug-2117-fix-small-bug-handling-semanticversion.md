# Bug 2117: SemanticVersion.isSupportedBy ignores patch version, allowing false compatibility

**Commit**: d49bae6c9b · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/utils/SemanticVersion.java
**Category**: Logic error in condition

---

## Root Cause

`SemanticVersion.isSupportedBy(version)` was intended to return `true` only when `version` is greater than or equal to `this` within the same major line. The implementation compared major and minor correctly but omitted the patch-level comparison: `major == version.major && minor <= version.minor`. A version like `1.2.4` would incorrectly report that it is supported by `1.2.3` (a lower patch version), because `minor <= minor` (2 <= 2) passed without checking patch.

### Before / After

```java
// BEFORE
return major == version.major && minor <= version.minor;
```

```java
// AFTER
return major == version.major && minor <= version.minor && patch <= version.patch;
```

---

## How to Spot

When implementing multi-level version comparisons, enumerate all components of the version tuple. A chain of `&&` conditions that stops short of the least-significant component will produce false positives for versions that differ only in the omitted component.

---

## Test

Test updated: `SemanticVersionTest` added an assertion that `v2.isSupportedBy(v1)` returns `false` when v2 has a higher patch than v1.
