**Commit**: e9e03faf87 · **Ticket**: KAFKA-18454
**Subsystem**: .github/scripts
**Category**: Logic error in condition

**Root cause**
This patch includes some maintenance updates for Develocity. * Publish build scans to develocity.apache.org
* Update Develocity Gradle plugin to to 3.19
* Use `DEVELOCITY_ACCESS_KEY` to authenticate to `develocity.apache.org`

Reviewers: Chia-Ping Tsai <chia7712@gmail.com>, David Arthur <mumrah@gmail.com>

**Before/After**
```java
// Before
    base_url = "https://ge.apache.org/scans/tests"
    base_url = "https://ge.apache.org/scans/tests"
    BASE_URL = "https://ge.apache.org"
          develocity-access-key: ${{ secrets.GE_ACCESS_TOKEN }}
          develocity-access-key: ${{ secrets.GE_ACCESS_TOKEN }}
# them to ge.apache.org.
          develocity-access-key: ${{ secrets.GE_ACCESS_TOKEN }}
          develocity-access-key: ${{ secrets.GE_ACCESS_TOKEN }}
    id 'com.gradle.develocity' version '3.17.6'
    server = "https://ge.apache.org"

// After
    base_url = "https://develocity.apache.org/scans/tests"
    base_url = "https://develocity.apache.org/scans/tests"
    BASE_URL = "https://develocity.apache.org"
          develocity-access-key: ${{ secrets.DEVELOCITY_ACCESS_KEY }}
          develocity-access-key: ${{ secrets.DEVELOCITY_ACCESS_KEY }}
# them to develocity.apache.org.
          develocity-access-key: ${{ secrets.DEVELOCITY_ACCESS_KEY }}
          develocity-access-key: ${{ secrets.DEVELOCITY_ACCESS_KEY }}
    id 'com.gradle.develocity' version '3.19'
    server = "https://develocity.apache.org"
```

**How to spot**
When reviewing .github/scripts code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18454 publish build scans to develocity.apache.org (#18539).

**Test**
No test added.
