**Commit**: 46785f191f · **Ticket**: KAFKA-19966

**Subsystem**: gradle/dependencies.gradle

**Category**: Wrong constant/default value

**Root cause**
The project dependency on `commons-validator` was pinned to an outdated version that contained known security or correctness issues. The fix bumps the declared version from the old pin to `1.10.1`, aligning the binary license manifest accordingly. No source code logic changed; the bug is entirely in the declared dependency version.

**Before**
```groovy
// Before
commonsValidator: "commons-validator:commons-validator:1.9.0",
```

**After**
```groovy
// After
commonsValidator: "commons-validator:commons-validator:1.10.1",
```

**How to spot**
Inspect `gradle/dependencies.gradle` (or the project's dependency lockfile) for `commons-validator` and compare against the latest release; any version below `1.10.1` on a branch that has not back-ported security patches is suspect. Also check `LICENSE-binary` to verify it agrees with the declared version.

**Test**
Run the full Gradle build with the updated dependency and confirm no dependency-resolution conflicts or test failures are introduced.
