# Bug 0406: DESCRIBE Statement Emits Wrong Options for CUSTOM Indices

**Commit**: 87db9187c1 · **Ticket**: CASSANDRA-16482
**Subsystem**: src/java/org/apache/cassandra/schema/IndexMetadata.java
**Category**: Wrong constant / default value

---

## Root Cause

`IndexMetadata.toCqlString()` built a copy of the options map (`copyOptions`) with the internal `class` entry removed, intending to show only user-visible options in the `WITH OPTIONS = { ... }` clause. However, the code then appended `options` (the full, unfiltered map) instead of `copyOptions`. This meant `DESCRIBE INDEX` for a CUSTOM index always emitted all internal options including the class name, and the reconstructed DDL could differ from what the user originally wrote, breaking round-trip schema export.

### Before / After

```java
// BEFORE
if (!copyOptions.isEmpty())
    builder.append(" WITH OPTIONS = ")
           .append(options);       // wrong: full map including 'class' entry
```

```java
// AFTER
if (!copyOptions.isEmpty())
    builder.append(" WITH OPTIONS = ")
           .append(copyOptions);   // correct: filtered map without 'class' entry
```

---

## How to Spot

When a method derives a filtered or sanitized copy of a collection and then formats it for output, the most common mistake is referencing the original variable instead of the copy. In code review, whenever you see a `copy*` or `filtered*` variable created just before a string-building block, verify that the string builder actually uses the copy, not the original. This class of bug is invisible to the logic that produces the index but only manifests when the schema is exported or described.

---

## Test

`DescribeStatementTest.testDescribeWithCustomIndex()` creates two SASI indexes — one without user options and one with `{'is_literal': 'false'}` — and asserts that `DESCRIBE INDEX` returns the exact expected CQL string for each, including verifying the options clause is correct and not padded with internal keys.
