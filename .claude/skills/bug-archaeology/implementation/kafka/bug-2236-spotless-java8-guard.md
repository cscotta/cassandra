**Commit**: 91390a9726 · **Ticket**: KAFKA-16896
**Subsystem**: build (spotless)
**Category**: State not cleaned up

**Root cause**
The Spotless plugin was pinned at 6.14.0, which predated several Java formatter improvements. The build script also contained a guard `if (JavaVersion.current().isJava11() || ...)` that existed to skip Spotless on JDK 8, which is no longer supported. Leaving the guard in place required every contributor to read it and understand it was vestigial, adding cognitive overhead with no functional benefit.

**Before/After**
```groovy
// Before
if (JavaVersion.current().isJava11()
    || JavaVersion.current().isJava12() ...) {
    spotless { ... }
}
spotlessVersion = "6.14.0"

// After – guard removed, version bumped
spotless { ... }
spotlessVersion = "6.25.0"
```

**How to spot**
Build-script version guards that reference JDK versions older than the project's declared `minJavaVersion` are dead branches; remove them when bumping `minJavaVersion` to keep the build script minimal and auditable.

**Test**
No test added.
