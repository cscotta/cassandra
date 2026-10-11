**Commit**: 1a18985cba · **Ticket**: KAFKA-18947
**Subsystem**: shell/src
**Category**: Logic error in condition

**Root cause**
* Remove unused `raftManager` in `metadataShell`
* Enhance error message when no snapshot provided. * Since `raftManager` is removed, make `snapshot` a required argument. Result when no snapshot is given
```
$ ./bin/kafka-metadata-shell.sh
usage: kafka-metadata-shell [-h] --snapshot SNAPSHOT [command [command ...]]
kafka-metadata-shell: error: argument --snapshot/-s is required
```
```
$ ./bin/kafka-metadata-shell.sh --help
usage: kafka-metadata-shell [-h] --snapshot SNAPSHOT [command [command ...]]

The Apache Kafka metadata shell

positional arguments:
  command                The command to run. optional arguments:
  -h, --help             show this help message and exit
  --snapshot SNAP...

**Before/After**
```java
// Before
import kafka.raft.KafkaRaftManager;
import org.apache.kafka.server.common.ApiMessageAndVersion;
        private KafkaRaftManager<ApiMessageAndVersion> raftManager = null;
        // Note: we assume that we have already taken the lock on the log directory before calling
        // this method.
        public Builder setRaftManager(KafkaRaftManager<ApiMessageAndVersion> raftManager) {
            this.raftManager = raftManager;
            return this;
        }

            return new MetadataShell(raftManager,
                snapshotPath,

// After
            return new MetadataShell(snapshotPath, faultHandler);

        initializeWithSnapshotFileReader();
```

**How to spot**
When reviewing shell/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18947 remove unused raftmanager in metadatashell (#19169).

**Test**
Test modified in MetadataShellIntegrationTest.java covering the corrected behaviour.
