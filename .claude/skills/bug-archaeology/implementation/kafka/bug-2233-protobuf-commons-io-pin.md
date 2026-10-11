**Commit**: 095ab0acba · **Ticket**: KAFKA-17753
**Subsystem**: build (dependencies, protobuf)
**Category**: Wrong constant / default value

**Root cause**
Protobuf was pinned to 3.23.4, which had known issues and was pulled in transitively by ZooKeeper without an explicit version pin in the clients module. `commons-io` was also brought in transitively through ZooKeeper without an explicit version, leaving its version uncontrolled. Upgrading protobuf to 3.25.5 and adding explicit `commons-io 2.14.0` and `protobuf` entries to `gradle/dependencies.gradle` gives the build reproducible dependency resolution.

**Before/After**
```groovy
// Before – no explicit protobuf or commons-io in clients
// (resolved transitively via ZooKeeper at unpinned versions)

// After
protobufVersion = "3.25.5"
commonsIoVersion = "2.14.0"
// explicitly declared in clients module dependencies
```

**How to spot**
Run `./gradlew dependencies | grep -E "protobuf|commons-io"` and verify that transitive versions match the pinned values; any mismatch indicates the explicit pin is missing or overridden.

**Test**
No test added.
