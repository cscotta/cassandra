**Commit**: fd00a3e58a · **Ticket**: KAFKA-16998
**Subsystem**: .github/workflows
**Category**: Logic error in condition

**Root cause**
Reviewers: Vedarth Sharma <vesharma@confluent.io>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
    - uses: actions/checkout@v3
      uses: actions/setup-python@v3
      uses: actions/upload-artifact@v3
      uses: actions/upload-artifact@v3
    - uses: actions/checkout@v3
      uses: actions/setup-python@v3
      uses: actions/upload-artifact@v3
      uses: actions/upload-artifact@v3
      uses: actions/setup-python@v3
        uses: actions/upload-artifact@v3
    - uses: actions/checkout@v3
      uses: actions/setup-python@v3

// After
    - uses: actions/checkout@v4
      uses: actions/setup-python@v5
      uses: actions/upload-artifact@v4
      uses: actions/upload-artifact@v4
    - uses: actions/checkout@v4
      uses: actions/setup-python@v5
      uses: actions/upload-artifact@v4
      uses: actions/upload-artifact@v4
      uses: actions/setup-python@v5
        uses: actions/upload-artifact@v4
    - uses: actions/checkout@v4
      uses: actions/setup-python@v5
```

**How to spot**
When reviewing .github/workflows code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16998 fix warnings in our github actions (#16410).

**Test**
Test modified in docker_build_and_test.yml covering the corrected behaviour.
