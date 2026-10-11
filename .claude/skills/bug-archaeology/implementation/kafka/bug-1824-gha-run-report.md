**Commit**: 369b8b5607 · **Ticket**: KAFKA-18223
**Subsystem**: .github/scripts
**Category**: Logic error in condition

**Root cause**
Run the flaky test report daily at 6am UTC. Reviewers: Jun Rao <junrao@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
    print("\nSummary of Most Problematic Tests")
    print("=" * 50)
    for class_name, details in problematic_tests.items():
                    'class': class_name,
                    'method': test_case.name.split('.')[-1],


    for class_name, cases in by_class.items():
        print(f"\n{class_name}")
                print(f"  → {method:<60} {case['failure_rate']:.2%}")
                print(f"  → Class-level flakiness rate: {case['failure_rate']:.2%}")


// After
    print("\n## Summary of Most Problematic Tests")
    if len(problematic_tests) > 0:
        print(f"Found {len(problematic_tests)} tests that have been quarantined for a while and are still flaky.")
    for full_class_name, details in problematic_tests.items():
            method_name = test_case.name.split('.')[-1]
                    'class': full_class_name,
                    'method': method_name,
    if len(flaky_regressions) > 0:
        print(f"Found {len(flaky_regressions)} tests that have started recently failing.")


    print("<table><tr><td>Class</td><td>Test Case</td><td>Failure Rate</td><td>Build Scans</td></tr>")
```

**How to spot**
When reviewing .github/scripts code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18223 add gha to run report [2/n] (#18170).

**Test**
No test added.
