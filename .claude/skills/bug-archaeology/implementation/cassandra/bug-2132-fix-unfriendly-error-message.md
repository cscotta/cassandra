# Bug 2132: CQL Grammar Produces NPE and Typo in Error Messages for Bad Collection Types

**Commit**: cc0c9f35b1 · **Ticket**: CASSANDRA-5132
**Subsystem**: src/java/org/apache/cassandra/cql3/Cql.g
**Category**: Missing null / bounds check

---

## Root Cause

When the ANTLR parser encountered an invalid type name inside a collection type declaration (e.g., `MAP<badtype, text>`), it would invoke `addRecognitionError` for the inner type but then attempt to call `ParsedType.Collection.map(t1, t2)` with one or both type references being `null` — ANTLR's recovery mechanism leaves the returned value as `null` after a recognition error. This resulted in a NullPointerException that swallowed the original user-friendly error. A separate cosmetic bug introduced a typo ("Errot" instead of "Error") in the `ConfigurationException` handler.

### Before / After

```java
// BEFORE — t1 or t2 may be null after ANTLR error recovery; NPE follows
{ try { $pt = ParsedType.Collection.map(t1, t2); }
  catch (InvalidRequestException e) { addRecognitionError(e.getMessage()); } }
```

```java
// AFTER — null check guards the factory call
{ try {
    if (t1 != null && t2 != null)
        $pt = ParsedType.Collection.map(t1, t2);
  } catch (InvalidRequestException e) { addRecognitionError(e.getMessage()); } }
```

---

## How to Spot

In ANTLR grammars, action blocks that call factory methods using rule return values must guard against `null`, because ANTLR's built-in error recovery can leave any `returns` variable unset after a recognition error. Treat every rule return value used inside `{ ... }` action blocks as potentially null when a sibling rule in the same production could fail.

---

## Test

No test added in this commit.
