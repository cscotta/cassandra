**Commit**: 7d2ad18520 · **Ticket**: KAFKA-19580
**Subsystem**: build.gradle / gradle/dependencies.gradle
**Category**: Wrong constant / default value

**Root cause**: Spotbugs 4.9.2 and 4.9.3 were broken on Java 24+ due to a regression (spotbugs/spotbugs#3311), so the project pinned to 4.9.1 and simultaneously excluded the `spotbugsMain`/`spotbugsTest` tasks when running on Java 24. This meant that static analysis was silently skipped on any Java 24 CI run. Upgrading to 4.9.4 (which fixes the regression) removes both the task-exclusion workaround and the version comment, restoring full analysis coverage.

**Before/After**:
```groovy
// BEFORE – tasks silently excluded on Java 24+
if (JavaVersion.current().isCompatibleWith(JavaVersion.VERSION_24)) {
    project.gradle.startParameter.excludedTaskNames.add("spotbugsMain")
    project.gradle.startParameter.excludedTaskNames.add("spotbugsTest")
}
// gradle/dependencies.gradle
spotbugs: "4.9.1"

// AFTER – exclusion removed; version bumped
// (no exclusion block)
spotbugs: "4.9.4"
```

**How to spot**: Any Gradle block that unconditionally excludes analysis tasks based on a runtime condition is a coverage gap. Search `excludedTaskNames` in build scripts; any task name there (especially test or analysis tasks) deserves scrutiny.

**Test**: No new functional test added. The fix is purely a build configuration change — subsequent CI runs on Java 24 will execute Spotbugs and surface any newly detected warnings, which were also suppressed in `spotbugs-exclude.xml` for classes not yet evaluated.
