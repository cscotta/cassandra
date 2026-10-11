# Bug 0096: Prepared Statement Size Computed for Unprepared Batch Statements

**Commit**: 78290bed45 · **Ticket**: CASSANDRA-20556
**Subsystem**: cql3 / transport
**Category**: Logic error in condition

---

## Root Cause

When `BatchMessage` processes queries that are still in raw (unprepared) string form, it calls `QueryProcessor.parseAndPrepare` to parse and compile them. Before this fix, that method unconditionally called `measurePstmnt` to record the serialized byte size of the prepared statement — a measurement only needed when the statement will be cached. Since unprepared batch statements are never stored in the prepared-statement cache, computing their size was unnecessary overhead and could mask size-related assumptions elsewhere. The fix adds a `measure` flag (defaulting to `true`) and passes `false` from the `BatchMessage` call site.

### Before / After

```java
// BEFORE (BatchMessage.java)
p = QueryProcessor.parseAndPrepare((String) query,
                                   state.getClientState().cloneWithKeyspaceIfSet(options.getKeyspace()),
                                   false);
// parseAndPrepare always: res.pstmntSize = measurePstmnt(res);
```

```java
// AFTER (BatchMessage.java)
p = QueryProcessor.parseAndPrepare((String) query,
                                   state.getClientState().cloneWithKeyspaceIfSet(options.getKeyspace()),
                                   false, false);  // measure=false: skip size computation
// parseAndPrepare now: if (measure) res.pstmntSize = measurePstmnt(res);
```

---

## How to Spot

Whenever a helper method computes an attribute whose sole purpose is to support caching, check every call site to verify that the statement will actually be cached. Call sites that create transient, non-cached objects should opt out of that measurement. A single unconditional computation path shared by both cached and uncached callers is a sign this logic should be parameterised or split.

---

## Test

No dedicated test added; the change removes an incorrect code path with no functional side-effect observable in existing tests.
