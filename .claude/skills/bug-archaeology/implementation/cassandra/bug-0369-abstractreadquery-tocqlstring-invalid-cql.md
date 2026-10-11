# Bug 0369: AbstractReadQuery::toCQLString Not Returning Valid CQL

**Commit**: e6a311f689 · **Ticket**: CASSANDRA-16510
**Subsystem**: `src/java/org/apache/cassandra/db/AbstractReadQuery.java` and related `db/filter/` classes
**Category**: Wrong serialization / deserialization

---

## Root Cause

`AbstractReadQuery.toCQLString()` was producing non-executable CQL strings in several ways: (1) keyspace and table names were emitted without quoting, so reserved-word or mixed-case names would produce invalid syntax; (2) partition-key values were rendered with `AbstractType.getString()` which returns the internal hex/debug form rather than a CQL literal; (3) clustering values similarly used `getString` instead of `toCQLString`; (4) a full-partition range (no restrictions at all) short-circuited to the string `"UNRESTRICTED"` instead of the actual WHERE clause from the row filter; and (5) `ALLOW FILTERING` was never appended, so the generated statement would be rejected at parse time whenever it was needed. The method is used in trace logging and diagnostics, so executing the returned string would silently fail.

### Before / After

```java
// BEFORE – unquoted names, no ALLOW FILTERING, range falls through to "UNRESTRICTED"
sb.append(metadata().keyspace).append('.').append(metadata().name);
// ...
if (isUnrestricted())
    return "UNRESTRICTED";   // not valid CQL
// ...
sb.append(type.getString(key));  // hex/debug form, not CQL literal
```

```java
// AFTER – quoted names, CQL-literal values, ALLOW FILTERING appended
sb.append(ColumnIdentifier.maybeQuote(metadata().keyspace)).append('.')
  .append(ColumnIdentifier.maybeQuote(metadata().name));
// ...
if (isUnrestricted(metadata))
    return rowFilter.toCQLString();   // valid WHERE clause or empty
// ...
sb.append(type.toCQLString(key));    // proper CQL literal
// ...
sb.append(" ALLOW FILTERING");
```

---

## How to Spot

Any `toString`/`toCQLString` helper that constructs a CQL fragment by concatenating raw internal values (type hex strings, unquoted identifiers) rather than calling the dedicated `toCQLString` method on each component is likely to produce non-executable CQL. During review, check that: (a) identifiers are wrapped with `ColumnIdentifier.maybeQuote`, (b) values use `AbstractType.toCQLString` not `getString`, and (c) all special-case early-return paths (e.g. "unrestricted") produce a string that is parseable by the CQL grammar.

---

## Test

`AbstractReadQueryToCQLStringTest` (818 lines added) exercises single-partition, partition-range, and virtual-table read commands across various key types, filters, and paging limits, asserting that the returned string can be re-parsed and re-executed against the cluster.
