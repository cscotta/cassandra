# Bug 2237: getColumnDefinition Confused Full Composite Column Name With Name Component

**Commit**: 981aa0578f · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/config/CFMetaData.java, src/java/org/apache/cassandra/cql/QueryProcessor.java, src/java/org/apache/cassandra/thrift/ThriftValidation.java
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

`CFMetaData.getColumnDefinition(ByteBuffer columnName)` did two different things depending on whether the comparator was composite: for simple column families it returned the definition keyed by the raw name, but for composite column families it parsed the first component out of the full column name and looked up by that component. Callers in the CQL `QueryProcessor` and `ThriftValidation` passed a full (internal) composite column name expecting the component-aware lookup, while the method signature gave no indication which argument convention was expected. This ambiguity caused wrong or null `ColumnDefinition` lookups, breaking column type validation for composite tables and causing `ColumnFamilyStoreTest` to fail.

The fix splits the method into two clearly named variants: `getColumnDefinition(ByteBuffer name)` performs a direct map lookup by the name as-is (suitable when the caller already has a definition key), and `getColumnDefinitionFromColumnName(ByteBuffer columnName)` performs the composite-aware component extraction. All existing callers that were passing full column names are updated to use the new `FromColumnName` variant.

### Before / After

```java
// BEFORE — single overloaded method, ambiguous convention
public ColumnDefinition getColumnDefinition(ByteBuffer columnName) { ... }
// callers unsure whether to pass component or full name
```

```java
// AFTER — two explicit methods
public ColumnDefinition getColumnDefinition(ByteBuffer name) {
    return column_metadata.get(name);
}
public ColumnDefinition getColumnDefinitionFromColumnName(ByteBuffer columnName) {
    // composite-aware extraction
}
```

---

## How to Spot

When a lookup method behaves differently based on an internal type check (simple vs. composite comparator) but its parameter name gives no hint of the expected convention, callers will guess wrong. During review, flag any API where the same `ByteBuffer` parameter can mean either "a definition key" or "a full column name" depending on CF type — the two cases must have distinct method names.

---

## Test

No test added (the commit description says it fixes a `ColumnFamilyStoreTest` failure, implying an existing test exercised the broken path).
