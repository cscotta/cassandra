# Bug 0402: ColumnFilter::toString Not Returning a Valid CQL Fragment

**Commit**: 9c7b69e422 · **Ticket**: CASSANDRA-16483
**Subsystem**: src/java/org/apache/cassandra/db/filter/ColumnFilter.java
**Category**: Wrong serialization / deserialization

---

## Root Cause

`ColumnFilter.toString()` was the only rendering method, but it produced an internal debug representation (e.g., `[v1, s2[1:3]]`) rather than a valid CQL `SELECT`-list (e.g., `v1, s2[1:3]`). `ReadCommand.toCQLString()` called `columnFilter()` with implicit `.toString()`, so the reconstructed CQL fragment embedded the bracketed debug form, making the output unparseable as CQL. Additionally, column names with special characters were not quoted: the method called `String.valueOf(column.name)` instead of `column.name.toCQLString()`, so identifiers like `Escaped Name` appeared unquoted and would cause a parse error.

### Before / After

```java
// BEFORE (ReadCommand.toCQLString)
sb.append("SELECT ").append(columnFilter());   // calls toString(), returns "[v1]"
sb.append(" FROM ").append(metadata().ksName).append('.').append(metadata.cfName);

// BEFORE (ColumnFilter.toString)
joiner.add(String.valueOf(column.name));       // no quoting
```

```java
// AFTER (ReadCommand.toCQLString)
sb.append("SELECT ").append(columnFilter().toCQLString());  // returns "v1"
sb.append(" FROM ").append(metadata().ksName).append('.').append(metadata().cfName);

// AFTER (new ColumnFilter.toCQLString / toString helper)
String columnName = cql ? column.name.toCQLString() : String.valueOf(column.name);
joiner.add(columnName);  // quotes if needed when cql=true
```

---

## How to Spot

When a class has both a debug `toString()` and a context-specific string-rendering need, callers that rely on `toString()` implicitly (e.g., string concatenation or `append()`) will silently produce wrong output. Review every call site where a domain object is appended to a `StringBuilder` that builds a query or protocol message, and verify that a dedicated, correctly escaping method is used rather than the default `toString()`.

---

## Test

`ColumnFilterTest.testSelectEscapedColumn()` selects a column named `"Escaped Name"` and asserts that `toCQLString()` returns `"\"Escaped Name\""` (quoted) while `toString()` returns the unquoted internal form.
