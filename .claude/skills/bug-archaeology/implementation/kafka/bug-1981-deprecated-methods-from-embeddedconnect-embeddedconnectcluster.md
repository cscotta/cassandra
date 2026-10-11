**Commit**: 337e2e1506 · **Ticket**: KAFKA-18037
**Subsystem**: connect/runtime
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
    /**
     * Execute a GET request on the given URL.
     *
     * @param url the HTTP endpoint
     * @return the response to the GET request
     * @throws ConnectException if execution of the GET request fails
     * @deprecated Use {@link #requestGet(String)} instead.
     */
    @Deprecated
    public String executeGet(String url) {
        return responseToString(requestGet(url));
    }

// After
// (no additions)
```

**How to spot**
When reviewing connect/runtime code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18037 remove deprecated methods from embeddedconnect and embeddedconnectcluster (#17857).

**Test**
Test modified in EmbeddedConnect.java covering the corrected behaviour.
