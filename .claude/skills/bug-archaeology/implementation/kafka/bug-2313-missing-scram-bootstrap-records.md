**Commit**: 16cac97873 · **Ticket**: KAFKA-17636
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Fixes a regression introduced by #16669 which inadvertently stopped processing SCRAM arguments from kafka-storage.sh

Reviewers: Colin P. McCabe <cmccabe@apache.org>, Federico Valeri <fedevaleri@gmail.com>

**Before/After**
```java
// Before
import java.util.Properties

// After
    Option(namespace.getList("add_scram")).
      foreach(scramArgs => formatter.setScramArguments(scramArgs.asInstanceOf[util.List[String]]))
import java.util.{Optional, Properties}
import org.apache.kafka.common.metadata.UserScramCredentialRecord
import org.apache.kafka.metadata.bootstrap.BootstrapDirectory
import scala.jdk.CollectionConverters.IterableHasAsScala

  @Test
  def testBootstrapScramRecords(): Unit = {
    val availableDirs = Seq(TestUtils.tempDir())
    val properties = new Properties()
    properties.putAll(defaultDynamicQuorumProperties)
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17636 fix missing scram bootstrap records (#17305).

**Test**
Test modified in StorageToolTest.scala covering the corrected behaviour.
