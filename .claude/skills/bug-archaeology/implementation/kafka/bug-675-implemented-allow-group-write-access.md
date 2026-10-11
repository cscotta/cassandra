**Commit**: bb0b62d961 · **Ticket**: KAFKA-19803
**Subsystem**: docs/streams
**Category**: Logic error in condition

**Root cause**
Implements KIP-1230. Reviewers: Matthias J. Sax <matthias@confluent.io>

**Before/After**
```java
// Before
          <tr class="row-even"><td>acceptable.recovery.lag</td>
import java.nio.file.attribute.PosixFilePermissions;
            final Set<PosixFilePermission> perms = PosixFilePermissions.fromString("rwxr-x---");

// After
          <tr class="row-odd"><td>acceptable.recovery.lag</td>
          <tr class="row-even"><td>allow.os.group.write.access</td>
              <td>Low</td>
              <td colspan="2">Allows state store directories created by Kafka Streams to have write access for the OS group.</td>
              <td><code class="docutils literal"><span class="pre">false</span></code></td>
          </tr>
    /** {@code allow.os.group.write.access} */
    @SuppressWarnings("WeakerAccess")
    public static final String ALLOW_OS_GROUP_WRITE_ACCESS_CONFIG = "allow.os.group.write.access";
    private static final String ALLOW_OS_GROUP_WRITE_ACCESS_DOC = "Allows state store directories created by Kafka Streams to have write access for the OS group. Default is false";

            .define(ALLOW_OS_GROUP_WRITE_ACCESS_CONFIG,
```

**How to spot**
When reviewing docs/streams code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19803: implemented support for allow.os.group.write.access config. (#20744).

**Test**
Test modified in StateDirectoryTest.java covering the corrected behaviour.
