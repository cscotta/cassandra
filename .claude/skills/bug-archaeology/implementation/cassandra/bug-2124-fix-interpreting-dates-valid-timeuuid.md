# Bug 2124: TimeUUIDType Accepts Plain Dates as Valid TimeUUID Values and Lacks Range-Query Helpers

**Commit**: 8be7e5c0eb · **Ticket**: CASSANDRA-4936
**Subsystem**: src/java/org/apache/cassandra/db/marshal/TimeUUIDType.java, src/java/org/apache/cassandra/utils/UUIDGen.java, src/java/org/apache/cassandra/cql3/statements/SelectStatement.java
**Category**: Wrong serialization / deserialization

---

## Root Cause

`TimeUUIDType.fromString()` silently accepted plain date strings (e.g., `"2013-01-18"`) as valid TimeUUID literals by converting them to a synthetic UUID. This was semantically wrong: a date is not a TimeUUID, and the synthetic UUID produced no uniqueness guarantees. The same method was used for both CQL2 (where the behaviour was kept for backward compatibility) and CQL3 (where it should be rejected). The fix splits the method into `fromStringCQL2` (retaining the lenient date handling) and a strict `fromString` (CQL3) that only accepts UUID-format strings or the new functional syntax `minTimeUUID(date)` / `maxTimeUUID(date)` / `now()`. Additionally, `UUIDGen` gained `minTimeUUID`, `maxTimeUUID`, and `unixTimestamp` helpers so that range queries can express "the smallest/largest UUID at a given millisecond" correctly, and `SelectStatement` gained `dateOf()` / `unixTimestampOf()` CQL functions.

### Before / After

```java
// BEFORE — fromString accepted dates silently
public ByteBuffer fromString(String source) throws MarshalException {
    // ... tried UUID parse, then fell through to date parse
}
```

```java
// AFTER — strict fromString rejects dates; CQL2 compat moved to fromStringCQL2
@Override
public ByteBuffer fromStringCQL2(String source) throws MarshalException { /* old logic */ }

public ByteBuffer fromString(String source) throws MarshalException {
    // only UUID regex or minTimeUUID()/maxTimeUUID()/now() functional syntax
}
```

---

## How to Spot

When a type's `fromString` method has a multi-branch parser that "helpfully" accepts a broader class of inputs than the type actually represents, it risks silently accepting invalid data. Whenever backward-compatibility lenience must be retained, factor it into a separate method rather than keeping a single catch-all parser.

---

## Test

No test added (validation is covered indirectly by CQL3 query tests in the broader test suite).
