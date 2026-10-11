# Bug 2173: CQL2 ALTER TABLE Ignored compaction_strategy_class Property

**Commit**: e30519f333 · **Ticket**: CASSANDRA-4965
**Subsystem**: src/java/org/apache/cassandra/cql/AlterTableStatement.java
**Category**: Silent skip on non-match

---

## Root Cause

`AlterTableStatement.applyPropertiesToCFMetadata()` in the CQL2 path checked for several `CFPropDefs` properties (`KW_COMPARATOR`, `KW_COMMENT`, `KW_DEFAULTVALIDATION`, etc.) but never checked for `KW_COMPACTION_STRATEGY_CLASS`. As a result, `ALTER TABLE … WITH compaction_strategy_class = '…'` statements were silently accepted by the parser but had no effect on the column family metadata — the compaction strategy was never changed.

### Before / After

```java
// BEFORE — KW_COMPACTION_STRATEGY_CLASS was never applied
if (cfProps.hasProperty(CFPropDefs.KW_COMPARATOR))
    throw new InvalidRequestException("Can't change CF comparator after creation");
if (cfProps.hasProperty(CFPropDefs.KW_COMMENT))
    cfm.comment(cfProps.getProperty(CFPropDefs.KW_COMMENT));
```

```java
// AFTER — compaction strategy is applied first
if (cfProps.hasProperty(CFPropDefs.KW_COMPACTION_STRATEGY_CLASS))
    cfm.compactionStrategyClass(cfProps.compactionStrategyClass);
if (cfProps.hasProperty(CFPropDefs.KW_COMPARATOR))
    throw new InvalidRequestException("Can't change CF comparator after creation");
if (cfProps.hasProperty(CFPropDefs.KW_COMMENT))
    cfm.comment(cfProps.getProperty(CFPropDefs.KW_COMMENT));
```

---

## How to Spot

When reviewing a property-dispatch block that handles a known set of configuration keys, verify that every key the parser accepts also has a corresponding handler in the apply method. Missing entries cause silent no-ops that are difficult to diagnose.

---

## Test

No test added.
