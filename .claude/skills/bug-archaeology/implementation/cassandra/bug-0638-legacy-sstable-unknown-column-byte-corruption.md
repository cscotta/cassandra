# Bug 0638: Legacy SSTable Reader Swallows UnknownColumnException, Corrupting bytesReadForNextAtom

**Commit**: 5378ba27ff · **Ticket**: CASSANDRA-13939
**Subsystem**: `db/LegacyLayout.java`, `db/UnfilteredDeserializer.java`
**Category**: Wrong serialization / deserialization

---

## Root Cause

`LegacyLayout.readLegacyAtom()` silently caught `UnknownColumnException` (for dropped columns) inside its own loop and retried from the top of the loop. This meant the method itself never returned the exception to callers. `UnfilteredDeserializer.readAtom()` wrapped the whole call in a single try/catch, measuring byte positions before and after to compute `bytesReadForNextAtom`. When an unknown cell was silently consumed inside `readLegacyAtom`, the byte advance was attributed to the *next* returned atom, not to the skipped cell — corrupting the position arithmetic used for streaming and paging.

The fix separates concerns: `readLegacyAtom` no longer loops internally, validates the skip is legitimate (system table or properly-registered dropped column), and re-throws `UnknownColumnException` for the caller. `UnfilteredDeserializer.readAtom()` now loops and catches the exception itself, ensuring `bytesReadForNextAtom` is reset correctly and the skipped bytes are excluded from the following atom's count.

### Before / After

```java
// BEFORE — exception swallowed inside readLegacyAtom; caller has no visibility
public static LegacyAtom readLegacyAtom(...) throws IOException {
    while (true) {
        ...
        try { return readLegacyCellBody(...); }
        catch (UnknownColumnException e) { /* silently skip */ }
    }
}
```

```java
// AFTER — exception propagated; caller decides to skip
public static LegacyAtom readLegacyAtom(...) throws IOException, UnknownColumnException {
    ...
    try { return readLegacyCellBody(...); }
    catch (UnknownColumnException e) {
        if (!isLegitimateSkip(metadata, e)) throw new IllegalStateException(...);
        throw e;   // let caller handle and reset byte counters
    }
}
```

---

## How to Spot

When a low-level deserialization method silently loops over exceptions to skip records, any byte-position tracking done by callers will be wrong: the skipped bytes get credited to the next record. Deserialization helpers should signal skippable records to their callers (via a sentinel return or a specific exception) rather than hiding the skip internally.

---

## Test

No test added (the fix is exercised by existing legacy-SSTable round-trip tests and the broader mixed-mode cluster regression suite).
