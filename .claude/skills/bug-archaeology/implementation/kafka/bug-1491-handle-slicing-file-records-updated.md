**Commit**: 484ba83f59 · **Ticket**: KAFKA-18683
**Subsystem**: clients/src
**Category**: Resource leak

**Root cause**
The PR corrects the check which was introduced in #5332 where position is checked to be within boundaries of file. The check 
    position > currentSizeInBytes - start 
is incorrect, since the position is relative to start. Reviewers: Jun Rao <junrao@gmail.com>

**Before/After**
```java
// Before
        if (position > currentSizeInBytes - start)

// After
        // position should always be relative to the start of the file hence compare with file size
        // to verify if the position is within the file.
        if (position > currentSizeInBytes)
import java.util.stream.IntStream;
    /**
     * Test slice when already sliced file records have start position greater than available bytes
     * in the file records.
     */
    @Test
    public void testSliceForAlreadySlicedFileRecords() throws IOException {
        byte[][] values = new byte[][] {
            "abcd".getBytes(),
```

**How to spot**
When reviewing clients/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18683: handle slicing of file records for updated start position (#18759).

**Test**
Test modified in FileRecordsTest.java covering the corrected behaviour.
