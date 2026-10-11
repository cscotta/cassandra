**Commit**: a738df4aaa · **Ticket**: KAFKA-18648
**Subsystem**: clients / FetchResponse protocol schema
**Category**: Wrong serialization / deserialization

**Root cause**
A prior change made the `Records` field in the `FetchResponse` schema non-nullable (removed `nullableVersions`). However several code paths in brokers running version 3.9 and earlier legitimately produce a null `records` field: ZSTD compression with fetch version < 10, down-conversion of zstandard-compressed batches, and certain uncaught exceptions. A 4.0 client therefore fails to deserialize fetch responses from older brokers because the generated parser rejects a null value.

**Before/After**
```json
// Before (FetchResponse.json)
{ "name": "Records", "type": "records", "versions": "0+",
  "about": "The record data."}

// After — nullable again for all versions
{ "name": "Records", "type": "records", "versions": "0+",
  "nullableVersions": "0+", "about": "The record data."}
```

**How to spot**
When tightening the schema of a protocol field from nullable to non-nullable, verify that every server code path that constructs the response actually populates the field. If any server version can produce a null value the client will break; prefer keeping nullable unless you control every response producer.

**Test**
No new test added for the schema change itself; the commit primarily reverts the schema and adds `nullableVersions`.
