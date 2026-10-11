**Commit**: fb19b3f7e7 · **Ticket**: KAFKA-14262
**Subsystem**: tools/MirrorMaker (v1)
**Category**: State not cleaned up

**Root cause**
MirrorMaker v1 (`kafka.tools.MirrorMaker` Scala class and accompanying shell scripts `kafka-mirror-maker.sh`) was deprecated since Kafka 3.0 in favour of MirrorMaker 2 (MM2 / `connect-mirror-maker.sh`). Keeping the dead code in the repository created a maintenance burden, confused new users who might reach for the old tool, and left deprecated Scala APIs that blocked other cleanup work. The fix deletes the Scala class, its unit tests, the shell scripts, and all references in documentation and configuration files.

**Before/After**
```scala
// Before: kafka.tools.MirrorMaker existed with --consumer.config,
// --producer.config, --whitelist, --num.streams, etc.

// After: class, shell scripts, and docs removed entirely.
// Users must use MirrorMaker 2 (connect-mirror-maker.sh).
```

**How to spot**
Deprecated classes that have a documented, fully-featured replacement should be removed on the next major version boundary. Check whether any shell scripts, documentation pages, or integration test fixtures still reference the deleted class; these must also be updated to avoid broken references.

**Test**
All `MirrorMakerTest` unit tests deleted along with the class.
