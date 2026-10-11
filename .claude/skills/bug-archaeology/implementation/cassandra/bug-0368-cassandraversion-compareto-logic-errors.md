# Bug 0368: CassandraVersion::compareTo Incorrect Ordering for Pre-Release and SNAPSHOT Labels

**Commit**: b42855282d · **Ticket**: CASSANDRA-16794
**Subsystem**: `src/java/org/apache/cassandra/utils/CassandraVersion.java`
**Category**: Logic error in condition

---

## Root Cause

`CassandraVersion.compareTo` had two separate defects in its pre-release identifier comparison logic. First, when the two pre-release identifier arrays differed in length, it always returned -1 (less) or +1 (greater) based purely on length without considering whether the extra segment was the special `SNAPSHOT` label — meaning `4.0.0-rc1-SNAPSHOT` was incorrectly ordered as *greater* than `4.0.0-rc1` instead of lesser. Second, the per-element string comparison used case-sensitive `compareTo` so `RC1` and `rc1` were treated as different, making `4.0.0-RC1` sort after `4.0.0-rc1` even though they should be equal. Additionally, the `familyLowerBound` helper (an empty pre-release array `new String[0]`) relied on a newly-created empty array for identity comparison rather than the canonical `ArrayUtils.EMPTY_STRING_ARRAY` constant, which could cause subtle equality checks to fail.

### Before / After

```java
// BEFORE – wrong: SNAPSHOT treated as a plain greater segment
if (ids1.length < ids2.length)
    return -1;
if (ids1.length > ids2.length)
    return 1;

int c = ids1[i].compareTo(ids2[i]);  // case-sensitive
```

```java
// AFTER – correct: SNAPSHOT makes the version lesser; comparison is case-insensitive
if (ids1.length < ids2.length)
{
    if (ids1.length == 0) return -1;
    return (ids2.length - ids1.length) == 1 && ids2[ids2.length - 1].equalsIgnoreCase("SNAPSHOT") ? 1 : -1;
}
if (ids1.length > ids2.length)
{
    if (ids2.length == 0) return 1;
    return (ids1.length - ids2.length) == 1 && ids1[ids1.length - 1].equalsIgnoreCase("SNAPSHOT") ? -1 : 1;
}

int c = ids1[i].compareToIgnoreCase(ids2[i]);
```

---

## How to Spot

Version-comparison utilities that parse structured multi-segment labels (semver pre-release, Maven qualifiers, etc.) often handle the equal-length path correctly but break on the length-differs path; whenever a "suffix means lesser" rule (e.g. SNAPSHOT, alpha, beta) is encoded as special-case logic, review all branches of the length-comparison block and verify that the suffix ordering is preserved even when the two strings have different segment counts. Also check whether any string comparisons in identifier arrays are inadvertently case-sensitive.

---

## Test

`CassandraVersionTest.testOrderWithSnapshotsAndFamilyLowerBound` shuffles a fully-ordered list of 30 version strings 100 times and asserts that `Collections.sort` produces the expected order every time.
