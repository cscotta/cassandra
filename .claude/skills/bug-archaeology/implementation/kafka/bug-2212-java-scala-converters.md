**Commit**: 76a1af984b · **Ticket**: KAFKA-17746
**Subsystem**: multiple Java modules (scala-compat migration)
**Category**: Wrong serialization / deserialization

**Root cause**
Java source files across the codebase imported `scala.collection.JavaConverters` via the Scala Java interop bridge. This deprecated API is removed in Scala 3 and generates warnings in 2.13. Because the import appeared in `.java` files rather than `.scala` files, the earlier Scala migration pass missed them. Each affected file needed its import replaced with `scala.jdk.javaapi.CollectionConverters`.

**Before/After**
```java
// Before (Java file)
import scala.collection.JavaConverters;
List<Foo> list = JavaConverters.seqAsJavaList(scalaSeq);

// After
import scala.jdk.javaapi.CollectionConverters;
List<Foo> list = CollectionConverters.asJava(scalaSeq);
```

**How to spot**
Run `grep -r "JavaConverters" --include="*.java"` to find Java files still using the deprecated Scala interop shim; these are easy to miss because most Scala migration tooling only processes `.scala` files.

**Test**
No test added.
