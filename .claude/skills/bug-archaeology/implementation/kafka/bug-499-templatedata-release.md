**Commit**: 3d46934cdd · **Ticket**: KAFKA-20069
**Subsystem**: gradle.properties
**Category**: Logic error in condition

**Root cause**
The `templateData.js` is removed by
https://github.com/apache/kafka/pull/21195. We should also remove
related paths. Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

Signed-off-by: PoAn Yang <payang@apache.org>

**Before/After**
```java
// Before
#  - docs/js/templateData.js
print("Updating docs templateData.js")
textfiles.replace(f"{repo_dir}/docs/js/templateData.js", "-SNAPSHOT", "", regex=True)

// After
// (no additions)
```

**How to spot**
When reviewing gradle.properties code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20069 remove templatedata.js in release.py (#21300).

**Test**
No test added.
