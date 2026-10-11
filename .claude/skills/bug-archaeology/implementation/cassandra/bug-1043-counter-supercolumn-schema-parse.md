# Bug 1043: Counter Columns in Thrift Supercolumns Rejected During Schema Parse

**Commit**: 18e2fca8b4 · **Ticket**: CASSANDRA-10365
**Subsystem**: Schema / CQL type system
**Category**: Logic error in condition

---

## Root Cause
`CQL3Type.Raw.Collection.prepare()` unconditionally threw `InvalidRequestException("Counters are not allowed inside collections")` whenever a counter type appeared as a collection value. This was correct for user-facing CQL, but Cassandra internally represents Thrift supercolumn families as `map<bytes, counter>` in the schema tables. When `CQLTypeParser.parse()` was called to reconstruct the type during schema loading, it hit this guard and failed to parse the stored schema for any counter supercolumn table. The fix introduces a separate `prepareInternal()` path that bypasses the counter-in-collection guard, used exclusively by the schema parser.

### Before / After
```java
// BEFORE — single prepare() that always blocks counters in collections
public CQL3Type prepare(String keyspace, Types udts) throws InvalidRequestException {
    if (values.isCounter())
        throw new InvalidRequestException("Counters are not allowed inside collections: " + this);
    ...
}

// AFTER — internal flag skips the guard when parsing stored schema
public CQL3Type prepare(String keyspace, Types udts, boolean isInternal) throws InvalidRequestException {
    if (values.isCounter() && !isInternal)
        throw new InvalidRequestException("Counters are not allowed inside collections: " + this);
    ...
}
// CQLTypeParser now calls prepareInternal() instead of prepare()
return parseRaw(unparsed).prepareInternal(keyspace, userTypes).getType();
```

---

## How to Spot
A validation rule that is correct for user input but must be relaxed for internal schema reconstruction is a common source of this pattern. In review, whenever a `prepare()` or `validate()` method is called from both user-facing and internal code paths, check whether all its invariants actually hold for internal representations (e.g., legacy Thrift mappings). If a stored schema format encodes something that would be rejected by the validator, the internal caller needs its own bypass.

---

## Test
No test added.
