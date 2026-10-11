**Commit**: bd27e34f2d · **Ticket**: KAFKA-18292
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
    @Deprecated
    public boolean dryRun() {
        return validateOnly;
    }

    @Deprecated
    public UpdateFeaturesOptions dryRun(boolean dryRun) {
        return validateOnly(dryRun);
    }


// After
                        <li>
                            The deprecated <code>dryRun</code> methods were removed from the <code>org.apache.kafka.clients.admin.UpdateFeaturesOptions</code>.
                            Please use <code>validateOnly</code> instead.
                        </li>
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18292 remove deprecated methods of updatefeaturesoptions (#18245).

**Test**
No test added.
