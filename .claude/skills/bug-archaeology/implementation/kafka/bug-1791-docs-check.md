**Commit**: 08efe735a4 · **Ticket**: KAFKA-18241
**Subsystem**: .github/workflows
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>, David Arthur <mumrah@gmail.com>

**Before/After**
```java
// Before
          ./gradlew --build-cache --info $SCAN_ARG check -x test

// After
          ./gradlew --build-cache --info $SCAN_ARG check siteDocTar -x test
      - name: Check generated documentation
        # Check if there are any empty files under ./site-docs/generated, If any empty files are found, print an error
        # message and list the empty files
        run: |
          tar zxvf core/build/distributions/kafka_2.13-$(./gradlew properties | grep version: | awk '{print $NF}' | head -n 1)-site-docs.tgz
          if find ./site-docs/generated -type f -exec grep -L "." {} \; | grep -q "."; then
            echo "One or more documentation files are empty!" >&2
            find ./site-docs/generated -type f -exec grep -L "." {} \; >&2
            exit 1
          fi
```

**How to spot**
When reviewing .github/workflows code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18241: add docs check to ci (#18183).

**Test**
No test added.
