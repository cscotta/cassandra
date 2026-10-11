**Commit**: 0727f9be63 · **Ticket**: KAFKA-18416
**Subsystem**: .github/scripts
**Category**: Logic error in condition

**Root cause**
Reviewers: David Arthur <mumrah@gmail.com>

**Before/After**
```java
// Before
            method_name = test_method.name.split('.')[-1]
                for entry in sorted(test_method.timeline, key=lambda x: x.timestamp)[-5:]:
        for entry in sorted(details['recent_executions'], k

// After
    def get_persistent_failing_tests(self, results: List[TestResult],
                                   min_failure_rate: float = 0.2,
                                   min_executions: int = 5) -> Dict[str, Dict]:
        """
        Identify tests that have been consistently failing/flaky over time.
        Groups by test class and includes individual test cases.
        """
        persistent_failures = {}
        current_time = datetime.now(pytz.UTC)
        chunk_start = current_time - timedelta(days=7)  # Last 7 days for test cases

        # Group results by class
```

**How to spot**
When reviewing .github/scripts code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18416 - added a new report section to catch persistent flaky tests (#18434).

**Test**
No test added.
