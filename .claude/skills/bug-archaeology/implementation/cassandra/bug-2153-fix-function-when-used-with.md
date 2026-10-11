# Bug 2153: CQL3 token() Function Incorrectly Rejected When Used with Strings

**Commit**: a399a56443 · **Ticket**: CASSANDRA-5050
**Subsystem**: src/java/org/apache/cassandra/cql3/statements/SelectStatement.java
**Category**: Logic error in condition

---

## Root Cause

The `SelectStatement` range-bound parsing checked `t.getType() == Term.Type.STRING` to decide whether to attempt a string-to-token conversion, but the check did not exclude terms that were already token literals (`t.isToken == true`). Token terms are represented as strings in the AST but should be used directly as token values, not re-parsed through the string conversion path, causing the query to fail when token() was called with string arguments.

### Before / After

```java
// BEFORE
if (t.getType() == Term.Type.STRING)
{
    // attempt string-to-minimum-token conversion
```

```java
// AFTER
if (t.getType() == Term.Type.STRING && !t.isToken)
{
    // attempt string-to-minimum-token conversion
```

---

## How to Spot

When a condition gates conversion logic on a type check, verify it also excludes sub-cases that share the same type representation but have different semantics. Here, token terms and plain strings both have `Type.STRING` but require different treatment.

---

## Test

No test added.
