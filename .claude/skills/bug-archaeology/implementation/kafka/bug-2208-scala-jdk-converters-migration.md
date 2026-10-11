**Commit**: 582bb48e88 · **Ticket**: KAFKA-17748
**Subsystem**: core/raft/storage (Scala collections migration)
**Category**: Wrong serialization / deserialization

**Root cause**
Multiple Scala source files in the `core`, `raft`, and `storage` modules used the deprecated `scala.collection.JavaConverters` object instead of `scala.jdk.javaapi.CollectionConverters` and `scala.jdk.OptionConverters`. The deprecated API is scheduled for removal in Scala 3 and generates compiler warnings in Scala 2.13, creating technical debt and potential compile failures in future Scala upgrades.

**Before/After**
```scala
// Before
import scala.collection.JavaConverters._
map.asJava
option.asJava

// After
import scala.jdk.javaapi.CollectionConverters._
import scala.jdk.OptionConverters._
CollectionConverters.asJava(map)
OptionConverters.toJava(option)
```

**How to spot**
Run `grep -r "JavaConverters" --include="*.scala"` across the tree; any remaining hits after a Scala 2.13 migration should be replaced with the `scala.jdk` equivalents before upgrading to Scala 3.

**Test**
No test added.
