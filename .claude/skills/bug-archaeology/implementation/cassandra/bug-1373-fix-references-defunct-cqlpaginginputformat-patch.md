# Bug 1373: Hadoop and Pig Integrations Reference Defunct CqlPagingInputFormat Class

**Commit**: e550ea6021 · **Ticket**: CASSANDRA-8541
**Subsystem**: src/java/org/apache/cassandra/hadoop/pig/CqlStorage.java
**Category**: Wrong constant / default value

---

## Root Cause

`CqlPagingInputFormat` was removed from Cassandra's Hadoop integration layer and superseded by `CqlInputFormat`, but three consumers—the `WordCount` and `WordCountCounters` example programs and the Pig `CqlStorage` class—continued to reference the old class name as a hardcoded string or class literal. At runtime these jobs would fail with a `ClassNotFoundException`. The root vulnerability is that the class name was duplicated as a string constant (`DEFAULT_INPUT_FORMAT = "org.apache.cassandra.hadoop.cql3.CqlPagingInputFormat"`) rather than derived from the class object, so the compiler could not catch the stale reference.

### Before / After

```java
// BEFORE: CqlStorage constructor
DEFAULT_INPUT_FORMAT = "org.apache.cassandra.hadoop.cql3.CqlPagingInputFormat";
```

```java
// AFTER
DEFAULT_INPUT_FORMAT = "org.apache.cassandra.hadoop.cql3.CqlInputFormat";
```

---

## How to Spot

String-typed class names that are never resolved through `Class.forName()` at compile time are invisible to the compiler when the target class is renamed or removed. Prefer `ClassName.class.getName()` over string literals for class references stored in variables or configuration. When reviewing a class deletion, search the entire codebase for the fully qualified class name as a string, not just as an import or type reference.

---

## Test

No test added.
