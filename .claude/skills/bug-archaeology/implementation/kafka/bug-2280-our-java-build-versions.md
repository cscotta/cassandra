**Commit**: bbbf688f55 · **Ticket**: KAFKA-17684
**Subsystem**: .github/workflows
**Category**: Logic error in condition

**Root cause**
This updates the versions of Java we test on from 8 and 21 to 11 and 21. This also removes unnecessary Check and Compile Java variations. Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
    strategy:
      fail-fast: false
      matrix:
        java: [ 21, 17, 11, 8 ]
    name: Compile and Check Java ${{ matrix.java }}
          java-version: ${{ matrix.java }}
          name: check-reports-${{ matrix.java }}
        if: ${{ failure() && matrix.java == '21' }}
        if: ${{ failure() && matrix.java == '21' }}
        java: [ 17, 11 ]  # If we change these, make sure to adjust ci-complete.yml
        java: [ 17, 11 ]

// After
    name: Compile and Check Java
          java-version: 21
          name: check-reports
        if: failure()
        if: failure()
        java: [ 21, 11 ]  # If we change these, make sure to adjust ci-complete.yml
        java: [ 21, 11 ]
```

**How to spot**
When reviewing .github/workflows code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17684 update our java build versions (#17350).

**Test**
No test added.
