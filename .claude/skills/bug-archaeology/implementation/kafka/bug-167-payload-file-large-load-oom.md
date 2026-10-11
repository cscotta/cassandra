**Commit**: b5c468fd7c · **Ticket**: KAFKA-18115
**Subsystem**: tools/src/main/java/org/apache/kafka/tools/ProducerPerformance.java
**Category**: Missing null / bounds check

**Root cause**
`readPayloadFile` loaded the entire file into a `String` via `Files.readString(path)` and then split it with `String.split(delimiter)`. On large files (hundreds of megabytes) this approach created two large in-memory objects (the full string and the resulting array) and, more critically, caused a `NegativeArraySizeException` when the file contents exceeded `Integer.MAX_VALUE` characters, because `String.split` internally allocates an array sized by the split count, which can overflow to a negative value. The fix replaces `readString` + `split` with a streaming `Scanner` that applies the delimiter lazily, avoiding full in-memory materialisation of the file.

**Before/After**
```java
// Before: entire file read into string, then split → NegativeArraySizeException on large files
String[] payloadList = Files.readString(path).split(payloadDelimiter);
for (String payload : payloadList) {
    payloadByteList.add(payload.getBytes(StandardCharsets.UTF_8));
}

// After: streaming Scanner, delimiter applied lazily
try (Scanner payLoadScanner = new Scanner(path, StandardCharsets.UTF_8)) {
    payLoadScanner.useDelimiter(payloadDelimiter);
    while (payLoadScanner.hasNext()) {
        payloadByteList.add(payLoadScanner.next().getBytes(StandardCharsets.UTF_8));
    }
}
```

**How to spot**
Any call to `Files.readString` or `Files.readAllBytes` on an unbounded user-supplied file is a candidate. Performance-test tooling is especially prone because users deliberately supply large payload files to stress-test brokers. Switching to `Scanner` or `BufferedReader` with a delimiter-aware approach eliminates the memory spike and the integer-overflow crash.

**Test**
`testReadPayloadFileWithAlternateDelimiters`: creates temporary files with various delimiter styles and asserts the correct number and content of payload entries. `testCompareStringSplitWithScannerDelimiter`: verifies that the Scanner-based result matches `String.split` for small files where both approaches are correct.
