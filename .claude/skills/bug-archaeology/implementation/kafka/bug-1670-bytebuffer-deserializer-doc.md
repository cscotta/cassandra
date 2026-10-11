## Commit

- Hash: 3b38b016c8
- Tickets: KAFKA-17825
- Subject: KAFKA-17825: Document ByteBufferDeserializer behavior change for KIP-863

## Subsystem

clients / Deserializer / documentation

## Category

Logic error in condition

## Root cause

KIP-863 changed the contract of `ByteBufferDeserializer`: it previously returned a view over the underlying network buffer, but now returns a copy. Code that retained the returned `ByteBuffer` reference expecting it to stay valid after the next `poll()` would silently see its data overwritten. The change was not documented in either the class Javadoc or the upgrade guide, leaving migrating users without any warning.

## Before

No mention of the behavior change in `Deserializer.java` or `docs/upgrade.html`.

## After

`Deserializer.java` Javadoc and `docs/upgrade.html` both note that `ByteBufferDeserializer` now returns a copy and that code relying on the shared-buffer behavior must be updated.

## How to spot

Behavioral changes to public API contracts (especially `Deserializer`, `Serde`, serialization) that touch the returned object's lifetime or mutability without a corresponding entry in the upgrade guide and class Javadoc.

## Test

Documentation change only; behavior already covered by existing deserializer tests.
