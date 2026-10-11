**Commit**: ea7da09e53 · **Ticket**: KAFKA-17899
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
// (no removals)

// After
import java.util.stream.IntStream;
    @Test
    public void testRequiredMemoryAmountKnownWhenNotSet() {
        NetworkReceive receive = new NetworkReceive("0");
        assertFalse(receive.requiredMemoryAmountKnown(), "Memory amount should not be known before read.");
    }

    @Test
    public void testRequiredMemoryAmountKnownWhenSet() throws IOException {
        NetworkReceive receive = new NetworkReceive(128, "0");

        ScatteringByteChannel channel = Mockito.mock(ScatteringByteChannel.class);
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17899 add more unit tests for networkreceive (#17637).

**Test**
Test modified in NetworkReceiveTest.java covering the corrected behaviour.
