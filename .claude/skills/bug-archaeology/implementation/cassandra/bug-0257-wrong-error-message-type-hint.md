# Bug 0257: Wrong Error Message Refers to "Type Casts" Instead of "Type Hints"

**Commit**: 35826b7447 · **Ticket**: CASSANDRA-17915
**Subsystem**: src/java/org/apache/cassandra/cql3/functions/FunctionResolver.java
**Category**: Wrong constant / default value

---

## Root Cause

When `FunctionResolver` detected an ambiguous arithmetic operation involving bind markers, it instructed users to resolve the ambiguity via "type casts" — a term that does not correspond to the actual CQL syntax. CQL uses "type hints" (written as `(int) ?`), not casts. The mismatch between the error message and the actual syntax would leave users confused and unable to act on the error without consulting external documentation.

### Before / After

```java
// BEFORE
throw invalidRequest(
    "Ambiguous '%s' operation with args %s and %s: use type casts to disambiguate",
    OperationFcts.getOperator(name), providedArgs.get(0), providedArgs.get(1));
```

```java
// AFTER
throw invalidRequest(
    "Ambiguous '%s' operation with args %s and %s: use type hint to disambiguate, example '(int) ?'",
    OperationFcts.getOperator(name), providedArgs.get(0), providedArgs.get(1));
```

---

## How to Spot

Error messages that describe a resolution action should be verified against the actual syntax accepted by the parser. A copy-and-paste of "cast" terminology from languages like Java or Kotlin is a red flag in a CQL context, where the concept is called a type hint with a distinct positional syntax.

---

## Test

Existing `OperationFctsTest` assertions are updated to expect the corrected message text `"use type hint to disambiguate, example '(int) ?'"` across five ambiguous-operation scenarios.
