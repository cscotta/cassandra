# Bug 0436: Partial variant shredding drops unshredded fields when object has no serialized backing buffer

**Commit**: e40c2d653793c3ec27fee5be32de38f57c0dd22e · **Ticket**: #15087

## Detection Rule

When a serialization path iterates over an object's fields by reading from both a shredded map and an underlying serialized buffer, verify that the code handles the case where the object was constructed purely in memory (no serialized buffer) so that unshredded fields are not silently omitted.

## Code Smell

A `ShreddedObject` produced by `new ShreddedObject(metadata, unshreddedObject)` (where the second arg is another in-memory `ShreddedObject` rather than a `SerializedObject` backed by bytes) passed through a serialization path that iterates the unshredded side — the absence of a byte-buffer backing causes those fields to be skipped.

## Tags

- scope: project-specific
- category: silent-skip
- code-pattern: missing-null-check
- severity: data-loss
- subsystem: core/variants

## Root Cause

`ShreddedObject.SerializationState` builds the final serialized form by combining fields from its `shreddedFields` map with fields from the `unshredded` object. When the `unshredded` object is itself an in-memory `ShreddedObject` (constructed via `put()` rather than deserialized from bytes), the serialization state could not iterate its fields correctly, silently dropping them from the output. The variable rename in the constructor (`shreddedFields` -> `shredded`) was a side-effect of the fix that also corrected the field-iteration logic for the non-serialized path.

## Example

```java
// BEFORE — unshredded fields lost when partial is a ShreddedObject
ShreddedObject partial = new ShreddedObject(metadata, unshreddedShreddedObject);
partial.put("c", Variants.ofIsoDate("2024-10-12"));
partial.remove("b");
// serializing `partial` silently drops field "a" from unshreddedShreddedObject
```

```java
// AFTER — all fields from the in-memory unshredded object are preserved
// Fix ensures SerializationState correctly walks the unshredded ShreddedObject's fields
// regardless of whether they are backed by a serialized byte buffer
```
