**Commit**: 8150e7bf79 · **Ticket**: KAFKA-18223
**Subsystem**: .github/scripts
**Category**: Logic error in condition

**Root cause**
Add tables for all sections of the report and add links to Develocity. Reviewers: David Arthur <mumrah@gmail.com>

**Before/After**
```java
// Before
            # Calculate success rate
            # Check if the test meets clearing criteria
                    cleared_tests[result.name] = {
                        'result': result,
                        'success_rate': success_rate,
                        'total_executions': len(recent_executions),
                        'successful_runs': successful_runs,
                        'recent_executions': recent_executions[-min_executions:]
                    }
def print_summary(problematic_tests: Dict[str, Dict], flaky_regressions: Dict[str, Dict]):
    """Print a summary of the most problematic tests at the top of the report"""
    print("\n## Summary of Most Problematic Tests")

// After
        chunk_start = current_time - timedelta(days=7)  # Last 7 days for test cases
            # Calculate success rate at class level
            # Check if the test meets clearing criteria at class level
                    try:
                        # Get test case details
                        test_cases = self.get_test_case_details(
                            result.name,
                            project,
                            chunk_start,
                            current_time,
                            test_type="quarantinedTest"
                        )
```

**How to spot**
When reviewing .github/scripts code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18223 improve flaky test report (#18212).

**Test**
No test added.
