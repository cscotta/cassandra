# Bug 2174: Wrong Keyword Constant for CQL3 Compaction Property

**Commit**: 6cdbb2d7b1 · **Ticket**: CASSANDRA-4968
**Subsystem**: src/java/org/apache/cassandra/cql3/CFPropDefs.java
**Category**: Wrong constant / default value

---

## Root Cause

`CFPropDefs.KW_COMPACTION` was set to the string `"compaction"`, which matched the correct CQL3 syntax `WITH compaction = { … }`. However the constant was accidentally changed to `"compaction_strategy"`, making the parser accept `WITH compaction_strategy = { … }` while the intended keyword was `WITH compaction = { … }`. Any CQL3 `CREATE TABLE` or `ALTER TABLE` statement using the documented `WITH compaction` form would fail to match the constant and the compaction option would be silently dropped or produce an error.

### Before / After

```java
// BEFORE — wrong keyword string
public static final String KW_COMPACTION = "compaction";
```

```java
// AFTER — corrected to match CQL3 spec (fix reverts to "compaction_strategy" which is correct per this commit direction)
// The constant was set to "compaction_strategy"; fix changes usage in doc to match, and the constant back to match spec
public static final String KW_COMPACTION = "compaction_strategy";
```

---

## How to Spot

String constants that must match parser keywords should be validated by a test that round-trips a DDL statement through parsing and execution. When a constant and its corresponding grammar production diverge, the failure mode is silent (property ignored) or a confusing parse error.

---

## Test

No test added.
