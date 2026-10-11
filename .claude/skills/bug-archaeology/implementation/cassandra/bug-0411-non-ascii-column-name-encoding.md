# Bug 0411: Column Names and UDT Field Names Encoded as ASCII Instead of UTF-8 in Native Protocol

**Commit**: f32475a839 · **Ticket**: CASSANDRA-16429
**Subsystem**: src/java/org/apache/cassandra/cql3/ResultSet.java, src/java/org/apache/cassandra/transport/DataType.java
**Category**: Wrong serialization / deserialization

---

## Root Cause

When serializing result-set column metadata and UDT definitions over the native protocol, column names and UDT field names were written with `CBUtil.writeAsciiString` (and sized with `sizeOfAsciiString`). ASCII encoding silently truncates or corrupts any non-ASCII characters. Because CQL permits non-ASCII characters in quoted identifiers (except for keyspace and table names), any column or UDT field name containing characters outside ASCII 0–127 would be sent incorrectly to the client, causing the driver to either reject the response or misinterpret the column name.

### Before / After

```java
// BEFORE — column name and UDT field name written as ASCII
CBUtil.writeAsciiString(name.name.toString(), dest);
CBUtil.writeAsciiString(udt.fieldName(i).toString(), cb);
```

```java
// AFTER — written as UTF-8
CBUtil.writeString(name.name.toString(), dest);
CBUtil.writeString(udt.fieldName(i).toString(), cb);
```

---

## How to Spot

Wherever a string that may originate from user-supplied identifiers is serialized over the wire, prefer the generic string encoder over any ASCII-specific variant. Reserve ASCII encoding only for values that are definitively restricted to ASCII by the protocol specification (e.g., keyspace and table names).

---

## Test

`SerDeserTest` was extended to generate random non-ASCII column names and UDT type/field names and verify that they survive a round-trip encode/decode cycle.
