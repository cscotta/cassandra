**Commit**: d7e5d0a59b · **Ticket**: KAFKA-18064
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
SASL mechanisms that do support neither integrity nor confidentality should throw exception on wrap/unwrap. The current implementation does not implement wrap/unwrap correctly. This may cause security issues, if the code using the mechanisms does
not check for QOP correctly. Reviewers: Gaurav Narula <gaurav_narula2@apple.com>, Igor Soarez <i@soarez.me>

**Before/After**
```java
// Before
import java.util.Arrays;
        return Arrays.copyOfRange(incoming, offset, offset + len);
        return Arrays.copyOfRange(outgoing, offset, offset + len);
import java.util.Arrays;
        return Arrays.copyOfRange(incoming, offset, offset + len);
        return Arrays.copyOfRange(outgoing, offset, offset + len);
import java.util.Arrays;
        return Arrays.copyOfRange(incoming, offset, offset + len);
        return Arrays.copyOfRange(outgoing, offset, offset + len);
        return Arrays.copyOfRange(incoming, offset, offset + len);

// After
        throw new IllegalStateException("OAUTHBEARER supports neither integrity nor privacy");
        throw new IllegalStateException("OAUTHBEARER supports neither integrity nor privacy");
        throw new IllegalStateException("OAUTHBEARER supports neither integrity nor privacy");
        throw new IllegalStateException("OAUTHBEARER supports neither integrity nor privacy");
        throw new IllegalStateException("PLAIN supports neither integrity nor privacy");
        throw new IllegalStateException("PLAIN supports neither integrity nor privacy");
        throw new IllegalStateException("SCRAM supports neither integrity nor privacy");
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18064: sasl mechanisms should throw exception on wrap/unwrap (#17901).

**Test**
No test added.
