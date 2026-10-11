# Bug 1361: Fix errant CPIF references

**Commit**: 0757dc72f2 · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/hadoop/pig/CqlStorage.java, examples/hadoop_cql3_word_count/
**Category**: Wrong constant / default value

---

## Root Cause

`CqlPagingInputFormat` (CPIF) was a deprecated Hadoop input format that had been superseded by `CqlInputFormat`. Multiple places — the Pig `CqlStorage` default input format string and the Hadoop CQL3 word-count examples — still referenced the old class name, either as a string constant or as a direct class reference. Any Hadoop job using `CqlStorage` or copying the word-count example would instantiate the obsolete paging format rather than the current one, picking up whatever behavioral differences (or outright ClassNotFoundErrors if CPIF was removed) existed between the two.

### Before / After

```java
// BEFORE (CqlStorage constructor)
DEFAULT_INPUT_FORMAT = "org.apache.cassandra.hadoop.cql3.CqlPagingInputFormat";

// BEFORE (WordCount example)
job.setInputFormatClass(CqlPagingInputFormat.class);
```

```java
// AFTER (CqlStorage constructor)
DEFAULT_INPUT_FORMAT = "org.apache.cassandra.hadoop.cql3.CqlInputFormat";

// AFTER (WordCount example)
job.setInputFormatClass(CqlInputFormat.class);
```

---

## How to Spot

When a class is renamed or superseded, a global search for the old name (as both a string literal and an import/reference) is necessary — string-typed class names are not caught by the compiler and can survive refactors indefinitely. Code reviews on deprecation or removal commits should explicitly audit all callers, including documentation and example files, for lingering references to the old identifier.

---

## Test

No test added.
