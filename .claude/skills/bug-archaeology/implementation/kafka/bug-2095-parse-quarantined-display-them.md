**Commit**: cbf440dfd0 · **Ticket**: KAFKA-17767
**Subsystem**: .github/scripts
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
def module_path_from_report_path(base_path: str, report_path: str) -> str:
        build/junit-xml/module[/sub-module]/[suite]/TEST-class.method.xml
    return os.path.join(*path_segments[0:-2])
            module_path = module_path_from_report_path(args.path, report)
            logger.debug(f"Parsing file: {report}, module: {module_path}")
                all_suite_passed = {test.key() for test in suite.passed_tests}
                flaky = all_suite_passed & all_suite_failed.keys()
                all_tests = all_suite_passed | all_suite_failed.keys()
    # Print summary
        def dest = rootProject.layout.buildDirectory.dir("junit-xml/${moduleDirPath}")

// After
QUARANTINED = "QUARANTINED 😷"
def split_report_path(base_path: str, report_path: str) -> Tuple[str, str]:
        build/junit-xml/module[/sub-module]/[task]/TEST-class.method.xml

    Returns a tuple of (module, task)

    return os.path.join(*path_segments[0:-2]), path_segments[-2]
    quarantined_table = []
            module_path, task = split_report_path(args.path, report)
            logger.debug(f"Parsing file: {report}, module: {module_path}, task: {task}")
                all_suite_passed = {test.key(): test for test in suite.passed_tests}
                flaky = all_suite_passed.keys() & all_suite_failed.keys()
```

**How to spot**
When reviewing .github/scripts code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17767 parse quarantined tests and display them [4/n] (#17661).

**Test**
No test added.
