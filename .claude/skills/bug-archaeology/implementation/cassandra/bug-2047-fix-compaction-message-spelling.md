# Bug 2047: Typo "Unkown" in OperationType.UNKNOWN Enum Label

**Commit**: 868e7eb3c8 · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/db/compaction/OperationType.java
**Category**: Wrong constant / default value

---

## Root Cause

The `OperationType.UNKNOWN` enum constant carried the display string `"Unkown compaction type"` — a misspelling of "Unknown". This string surfaces in compaction progress reporting and JMX output visible to operators, making it confusing and unprofessional. The fix corrects it to `"Unknown compaction type"`.

### Before / After

```java
// BEFORE
UNKNOWN("Unkown compaction type");
```

```java
// AFTER
UNKNOWN("Unknown compaction type");
```

---

## How to Spot

Review enum constant display strings and log message string literals for spelling. The `UNKNOWN` sentinel value in enums is easy to overlook because it is often added as a defensive fallback and not exercised by normal code paths, so its message string may never be visually inspected after initial authorship.

---

## Test

No test added.
