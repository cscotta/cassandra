**Commit**: 02f541d4ea · **Ticket**: KAFKA-16518
**Subsystem**: checkstyle/import-control-metadata.xml
**Category**: Logic error in condition

**Root cause**
As part of KIP-853, storage-tool.sh now has two new flags: --standalone, and --initial-voters. This PR implements these two flags in storage-tool.sh. There are currently two valid ways to format a cluster:

The pre-KIP-853 way, where you use a statically configured controller quorum. In this case, neither --standalone nor --initial-voters may be specified, and kraft.version must be set to 0.

**Before/After**
```java
// Before
              files="RecordsIterator.java"/>
              files="(PartitionRegistration|PartitionChangeBuilder).java"/>
import net.sourceforge.argparse4j.inf.Namespace
import org.apache.kafka.metadata.bootstrap.{BootstrapDirectory, BootstrapMetadata}
import org.apache.kafka.server.common.{ApiMessageAndVersion, Features, MetadataVersion}
import org.apache.kafka.common.metadata.FeatureLevelRecord
import org.apache.kafka.common.metadata.UserScramCredentialRecord
import org.apache.kafka.common.security.scram.internals.ScramMechanism
import org.apache.kafka.common.security.scram.internals.ScramFormatter
import org.apache.kafka.server.config.ReplicationConfigs
import org.apache.kafka.metadata.properties.MetaPropertiesEnsemble.VerificationFlag
import org.apache.kafka.server.common.FeatureVersion

// After
        <subpackage name="storage">
            <allow pkg="org.apache.kafka.common.internals" />
            <allow pkg="org.apache.kafka.snapshot" />
        </subpackage>
              files="(DynamicVoter|RecordsIterator).java"/>
              files="(PartitionRegistration|PartitionChangeBuilder|ScramParser).java"/>
import net.sourceforge.argparse4j.inf.{ArgumentParserException, Namespace}
import org.apache.kafka.server.common.MetadataVersion
import org.apache.kafka.metadata.storage.{Formatter, FormatterException}
import org.apache.kafka.raft.DynamicVoters
import org.apache.kafka.server.ProcessRole
import org.apache.kafka.server.config.ReplicationConfigs
```

**How to spot**
When reviewing checkstyle/import-control-metadata.xml code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16518: implement kip-853 flags for storage-tool.sh (#16669).

**Test**
Test modified in DelegationTokenEndToEndAuthorizationTest.scala covering the corrected behaviour.
