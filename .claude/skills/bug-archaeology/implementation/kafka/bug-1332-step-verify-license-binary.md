**Commit**: a6064e0800 · **Ticket**: KAFKA-17607
**Subsystem**: .github/workflows
**Category**: Logic error in condition

**Root cause**
Add the verify_license.py script to our build to detect missing licenses. Reviewers: Chia-Ping Tsai <chia7712@gmail.com>, Ken Huang <s7133700@gmail.com>, David Arthur <mumrah@gmail.com>

**Before/After**
```java
// Before
          ./gradlew --build-cache --info $SCAN_ARG check siteDocTar -x test
    pattern = re.compile(r'^kafka_2\.13-.+\.tgz$', re.IGNORECASE)

    # Build the tarball.
    run_gradlew(project_dir)

// After
          ./gradlew --build-cache --info $SCAN_ARG check releaseTarGz -x test
      - name: Verify license file
        run: python committer-tools/verify_license.py --skip-build
import argparse
    pattern = re.compile(r'^kafka_2\.13-(?!.*docs).+\.tgz$', re.IGNORECASE)
    # Argument parser
    parser = argparse.ArgumentParser(description="Whether to skip executing ReleaseTarGz.")
    parser.add_argument("--skip-build", action="store_true", help="skip the build")
    args = parser.parse_args()


    if args.skip_build:
```

**How to spot**
When reviewing .github/workflows code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17607: add ci step to verify license-binary (#18299).

**Test**
No test added.
