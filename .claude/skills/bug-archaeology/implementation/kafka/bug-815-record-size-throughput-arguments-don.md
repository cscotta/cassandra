**Commit**: eeb6a0d981 · **Ticket**: KAFKA-19618
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
The `record-size` and `throughput` arguments don’t work in
`TestRaftServer`. The `recordsPerSec` and `recordSize` values are always
hard-coded. - Fix `recordsPerSec` and `recordSize` values hard-coded issue
- Add "Required" description to command-line options to make it clear to
users. Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
import org.apache.kafka.common.errors.InvalidConfigurationException
      recordsPerSec = 20000,
      recordSize = 256
    val configOpt: OptionSpec[String] = parser.accepts("config", "Required configured file")
    val directoryId: OptionSpec[String] = parser.accepts("replica-directory-id", "The directory id of the replica")
      if (configFile == null) {
        throw new InvalidConfigurationException("Missing configuration file. Should specify with '--config'")
      }

      if (directoryIdAsString == null) {
        throw new InvalidConfigurationException("Missing replica directory id. Should specify with --replica-directory-id")
      }

// After
      recordsPerSec = throughput,
      recordSize = recordSize
    val configOpt: OptionSpec[String] = parser.accepts("config", "REQUIRED: The configured file")
    val directoryId: OptionSpec[String] = parser.accepts("replica-directory-id", "REQUIRED: The directory id of the replica")

    def checkArgs(): Unit = CommandLineUtils.checkRequiredArgs(parser, options, configOpt, directoryId)
      opts.checkArgs()

```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19618 the `record-size` and `throughput`arguments don't work in testraftserver (#20379).

**Test**
Test modified in TestRaftServer.scala covering the corrected behaviour.
