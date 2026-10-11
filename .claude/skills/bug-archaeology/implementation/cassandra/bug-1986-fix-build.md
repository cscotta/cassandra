# Bug 1986: Missing Semicolon Breaks Build in DatabaseDescriptor

**Commit**: d6145668c6 · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/config/DatabaseDescriptor.java
**Category**: Logic error in condition

---

## Root Cause

A missing semicolon at the end of the `return` statement in `DatabaseDescriptor.isReplacing()` caused a compilation failure. The statement spanned multiple logical conditions and the terminator was accidentally omitted, preventing the project from building.

### Before / After

```java
// BEFORE: missing semicolon — compilation error
public static boolean isReplacing()
{
    return 0 != getReplaceTokens().size() || getReplaceNode() != null
}
```

```java
// AFTER
public static boolean isReplacing()
{
    return 0 != getReplaceTokens().size() || getReplaceNode() != null;
}
```

---

## How to Spot

Multi-line boolean return expressions are a common place for missing terminators. Code review tools and CI that compiles every commit catch these immediately.

---

## Test

No test added; the fix is a compilation error correction.
