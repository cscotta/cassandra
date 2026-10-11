**Commit**: 5af84cf1e1 · **Ticket**: KAFKA-20262
**Subsystem**: streams/src/main/java/org/apache/kafka/streams/state/internals/ (ChangeLoggingTimestampedKeyValueBytesStoreWithHeaders, MeteredSessionStoreWithHeaders, MeteredTimestampedKeyValueStoreWithHeaders)
**Category**: Missing override of default method

**Root cause**
The headers-aware changelog store wrappers (`ChangeLoggingTimestampedKeyValueBytesStoreWithHeaders`, `MeteredTimestampedKeyValueStoreWithHeaders`, `MeteredSessionStoreWithHeaders`) did not override `delete()` or `remove()`. The default implementations in the parent classes called the wrapped store's `delete`/`remove` and then wrote a tombstone record to the changelog topic — but without propagating the custom headers from the current record context. Tombstones thus lost all custom headers, causing header-aware downstream consumers or processors to receive incomplete information on deletions.

**Before/After**
```java
// Before: no delete() override in ChangeLoggingTimestampedKeyValueBytesStoreWithHeaders
// → inherited default writes tombstone without headers

// After: explicit override that passes current record context headers
@Override
public byte[] delete(final Bytes key) {
    final byte[] deletedValue = wrapped().delete(key);
    log(key, null, internalContext.recordContext().timestamp(),
        internalContext.recordContext().headers());
    return deletedValue;
}
```

**How to spot**
Any headers-aware `Wrapped*Store` subclass that does not override all mutating methods (`put`, `delete`, `remove`) is a candidate. Search for classes extending changelog-aware store wrappers and check each mutating method for a `log(key, value, timestamp, headers)` call with the correct headers.

**Test**
New tests verify that after a `delete()` or `remove()` call, the tombstone record written to the changelog topic carries the same custom headers that were set on the input record context.
