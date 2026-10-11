**Commit**: 16359e70d3 · **Ticket**: KAFKA-16583
**Subsystem**: metadata/src
**Category**: Resource leak

**Root cause**
When PartitionRegistration#merge() reads a PartitionChangeRecord
from an older MetadataVersion, with a replica assignment change
and without #directories() set, it produces a direcotry assignment
of DirectoryId.UNASSIGNED. This is problematic because the MetadataVersion
may not yet support directory assignments, leading to a
UnwritableMetadataException in PartitionRegistration#toRecord. Since the Controller always sets directories on PartitionChangeRecord
if the MetadataVersion supports it, via PartitionChangeBuilder,
there's no need for PartitionRegistration#merge() to populate
directories upon a replica assignment change. Reviewers: Luke Chen <showuon@gmail.com>

**Before/After**
```java
// Before
        if (directories == null || directories.length == 0) {
        Uuid[] newDirectories;
        if (record.directories() != null && !record.directories().isEmpty()) {
            newDirectories = Uuid.toArray(checkDirectories(record));
        } else if (record.replicas() != null) {
            newDirectories = Uuid.toArray(DirectoryId.createDirectoriesFrom(replicas, directories, record.replicas()));
        } else {
            newDirectories = directories;
        }
            defaultToMigrating(newDirectories, replicas.length),
            setReplicas(Arrays.asList(1, 2, 3, 4)));

// After
        .setDirectories(util.Arrays.asList(Uuid.fromString("fKgQ2axkQiuzt4ANqKbPkQ"), DirectoryId.UNASSIGNED, DirectoryId.UNASSIGNED))
    private static boolean migratingDirectories(Uuid[] directories) {
        if (directories == null) {
            return true;
        }
        for (Uuid directory : directories) {
            if (!DirectoryId.MIGRATING.equals(directory)) {
                return false;
            }
        }
        return true;
    }
```

**How to spot**
When reviewing metadata/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16583: handle partitionchangerecord without directory ids (#16118).

**Test**
Test modified in ReplicaManagerTest.scala covering the corrected behaviour.
