# Bug 2188: Collection DELETE/UPDATE only read-before-write for first list column

**Commit**: 25f8b6faeb · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/cql3/statements/DeleteStatement.java, src/java/org/apache/cassandra/cql3/statements/UpdateStatement.java, src/java/org/apache/cassandra/cql3/statements/ModificationStatement.java
**Category**: Silent skip on non-match

---

## Root Cause

When a CQL DELETE or UPDATE statement targeted multiple list columns requiring a read-before-write (e.g. deleting a list element by value), the code populated a `List<ByteBuffer> toRead` and immediately `break`ed after adding the first matching column name. This meant only the first list column was ever read back from storage; subsequent list columns were silently skipped. Additionally, using a `List` allowed duplicate column names, and the list-based iteration was not well-suited to building `ColumnSlice` ranges. The fix changes the collection to `TreeSet<ByteBuffer>` (ordered by `UTF8Type`) and removes the `break`, ensuring all list columns requiring a read are collected and iterated in a stable, deduplicated order.

### Before / After

```java
// BEFORE — only the first list column read
List<ByteBuffer> toRead = null;
for (Pair<CFDefinition.Name, Term> p : toRemove) {
    if ((name.type instanceof ListType) && value != null) {
        if (toRead == null)
            toRead = new ArrayList<ByteBuffer>();
        toRead.add(name.name.key);
        break;  // <-- silently skips all remaining list columns
    }
}
```

```java
// AFTER — all list columns collected
Set<ByteBuffer> toRead = null;
for (Pair<CFDefinition.Name, Term> p : toRemove) {
    if ((name.type instanceof ListType) && value != null) {
        if (toRead == null)
            toRead = new TreeSet<ByteBuffer>(UTF8Type.instance);
        toRead.add(name.name.key);
        // no break
    }
}
```

---

## How to Spot

A `break` inside a loop that is supposed to collect all matching items into a list is a red flag. The pattern "add one item then break" is only correct when you want the first match, not all matches. When multiple list columns in a single statement require a read, breaking early leaves subsequent columns in a stale state.

---

## Test

No test added.
