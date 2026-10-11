# Bug 0091: Mutation Serializer Reads Flags from Wrong Stream; AccordJournal Interop Entries Reference Wrong Message Type

**Commit**: 021b06639e · **Ticket**: n/a
**Subsystem**: db — Mutation.Serializer; service/accord — AccordJournal
**Category**: Wrong serialization / deserialization

---

## Root Cause

Two independent serialization bugs were fixed together. First, the `Mutation` deserializer added a `flags` byte for version >= VERSION_51, but read it from the plain `in` stream instead of the `teeIn` wrapper stream. Because the tee stream is the one whose position is actually tracked for downstream deserialization, reading from `in` left `teeIn` with an off-by-one byte offset, corrupting every subsequent field. Second, in `AccordJournal.Type`, the three interop journal entry types (`INTEROP_COMMIT_MAXIMAL`, `INTEROP_APPLY_MINIMAL`, `INTEROP_APPLY_MAXIMAL`) all listed `COMMIT_MINIMAL_REQ` as their fallback/equivalent message type rather than the correct `COMMIT_MAXIMAL_REQ`, `APPLY_MINIMAL_REQ`, and `APPLY_MAXIMAL_REQ` respectively, causing replayed journal messages to be decoded with the wrong serializer.

### Before / After

```java
// BEFORE — Mutation deserializer reads flags from wrong stream
if (version >= VERSION_51) {
    int flags = in.readByte();       // wrong: should be teeIn
    allowsOutOfRangeMutations = allowsOutOfRangeMutations(flags);
}
```

```java
// AFTER
if (version >= VERSION_51) {
    int flags = teeIn.readByte();    // correct tee stream
    allowsOutOfRangeMutations = allowsOutOfRangeMutations(flags);
}
```

```java
// BEFORE — AccordJournal.Type all interop entries use COMMIT_MINIMAL_REQ
INTEROP_COMMIT_MAXIMAL (91, ..., COMMIT_MINIMAL_REQ, ...),
INTEROP_APPLY_MINIMAL  (92, ..., COMMIT_MINIMAL_REQ, ...),
INTEROP_APPLY_MAXIMAL  (93, ..., COMMIT_MINIMAL_REQ, ...),
```

```java
// AFTER
INTEROP_COMMIT_MAXIMAL (91, ..., COMMIT_MAXIMAL_REQ, ...),
INTEROP_APPLY_MINIMAL  (92, ..., APPLY_MINIMAL_REQ,  ...),
INTEROP_APPLY_MAXIMAL  (93, ..., APPLY_MAXIMAL_REQ,  ...),
```

---

## How to Spot

When a serialization path wraps a raw stream in a tee or buffered decorator, every read must go through the decorator—never through the raw stream directly. A review pattern: grep for both variable names (`in` and `teeIn`, or equivalent) in the same method and flag any use of the non-tee variable after the decorator is created. For enum tables that map message types to serializers, verify that each enum entry's fallback type matches its semantic variant (minimal vs. maximal) rather than being copy-pasted from an earlier entry.

---

## Test

No dedicated test added for these serialization fixes; existing tests in `StorageServiceServerTest` and `AccordSyncPropagatorTest` were updated to compile after related refactoring.
