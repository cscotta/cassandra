**Commit**: ceab1fe658 · **Ticket**: KAFKA-16969
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
When broker configuration is incompatible with the current Metadata Version the Broker should log an error-level message but avoid shutting down. Reviewers: Luke Chen <showuon@gmail.com>

**Before/After**
```java
// Before
            RuntimeException exception = this.faultHandler.handleFault(
            if (exception != null) {
                throw exception;
            }

// After
    @SuppressWarnings("ThrowableNotThrown")
            this.faultHandler.handleFault(
import static org.mockito.ArgumentMatchers.any;
        when(faultHandler.handleFault(any(), any())).thenReturn(new RuntimeException("returned exception"));
        <li>
            JBOD support in KRaft was introduced from Metadata Version (MV) 3.7-IV2.
            Configuring Brokers with multiple log directories can lead to indefinite unavailability.
            Brokers will now detect this situation and log an error.
            See <a href="https://issues.apache.org/jira/browse/KAFKA-16606">KAFKA-16606</a> for more details.
        </li>
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16969: log error if config conficts with mv (#16366).

**Test**
Test modified in MetadataVersionConfigValidatorTest.java covering the corrected behaviour.
