**Commit**: 0294b1402d · **Ticket**: KAFKA-17479
**Subsystem**: .github/actions
**Category**: Logic error in condition

**Root cause**
For several modules, we include a kafka-version.properties in the Jar file. This file includes the Git SHA of the project at the time of the build. This means that even if no source files change, the :jar task will never be UP-TO-DATE between two git commits. Ultimately, this breaks Gradle caching.

**Before/After**
```java
// Before
          -PignoreFailures=true -PmaxParallelForks=2 \
println("Build properties: maxParallelForks=$maxTestForks, maxScalacThreads=$maxScalacThreads, maxTestRetries=$userMaxTestRetries")

// After
  gradle-cache-write-only:
    description: "Should the Gradle cache be write-only?"
    default: "false"
        cache-write-only: ${{ inputs.gradle-cache-write-only }}
    def __repr__(self):
        return f"{self.class_name} {self.test_name}"

      gradle-cache-write-only:
        description: "Should the Gradle cache be write-only?"
        default: "false"
        type: string
          gradle-cache-write-only: ${{ inputs.gradle-cache-write-only }}
```

**How to spot**
When reviewing .github/actions code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17479 allow ":jar" tasks to be cached [1/n] (#17066).

**Test**
No test added.
