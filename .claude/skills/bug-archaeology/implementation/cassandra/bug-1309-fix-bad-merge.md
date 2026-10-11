# Bug 1309: Bad Merge Leaves Wrong Checksum Class in DataIntegrityMetadata

**Commit**: 7bef6f93ae · **Ticket**: n/a
**Subsystem**: io/util/DataIntegrityMetadata
**Category**: Wrong constant / default value

---

## Root Cause

A merge conflict was resolved incorrectly, leaving `new PureJavaCrc32()` as the fallback checksum implementation instead of the intended `CRC32Factory.instance.create()`. The two expressions produce different objects: `PureJavaCrc32` is the old hand-rolled implementation, while `CRC32Factory.instance.create()` returns the factory-selected CRC32 (which may use a hardware-accelerated implementation). Using the wrong class would silently compute checksums with the old algorithm for any SSTable version that did not use Adler32, potentially causing checksum validation to fail against data written by a newer version, or vice versa.

### Before / After

```java
// BEFORE – wrong class after bad merge
checksum = descriptor.version.hasAllAdlerChecksums() ? new Adler32() : new PureJavaCrc32();
```

```java
// AFTER – correct factory-based selection
checksum = descriptor.version.hasAllAdlerChecksums() ? new Adler32() : CRC32Factory.instance.create();
```

---

## How to Spot

After resolving a merge conflict that involves a factory method or strategy selection, verify that the chosen expression matches the established factory pattern used elsewhere in the file. A direct `new ConcreteClass()` instantiation where every other callsite uses a factory is a strong indicator that a merge produced the wrong result.

---

## Test

No test added.
