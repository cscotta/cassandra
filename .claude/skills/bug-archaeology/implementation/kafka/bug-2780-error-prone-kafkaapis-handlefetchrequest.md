**Commit**: e57cbe0346 · **Ticket**: KAFKA-17022
**Subsystem**: core/src
**Category**: Resource leak

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
      var unconvertedFetchResponse: FetchResponse = null

      def createResponse(throttleTimeMs: Int): FetchResponse = {
        unconvertedFetchResponse = fetchContext.updateAndGenerateResponseData(partitions)
        requestHelper.sendResponseExemptThrottle(request, createResponse(0), Some(updateConversionStats))
        if (maxThrottleTimeMs > 0) {
          unconvertedFetchResponse = fetchContext.getThrottledResponse(maxThrottleTimeMs)
          unconvertedFetchResponse = fetchContext.updateAndGenerateResponseData(partitions)
        requestChannel.sendResponse(request, createResponse(maxThrottleTimeMs), Some(updateConversionStats))

// After
      def createResponse(throttleTimeMs: Int, unconvertedFetchResponse: FetchResponse): FetchResponse = {
        val unconvertedFetchResponse = fetchContext.updateAndGenerateResponseData(partitions)
        requestHelper.sendResponseExemptThrottle(request, createResponse(0, unconvertedFetchResponse), Some(updateConversionStats))
        val unconvertedFetchResponse = if (maxThrottleTimeMs > 0) {
          fetchContext.getThrottledResponse(maxThrottleTimeMs)
          val unconvertedFetchResponse = fetchContext.updateAndGenerateResponseData(partitions)
          unconvertedFetchResponse
        requestChannel.sendResponse(request, createResponse(maxThrottleTimeMs, unconvertedFetchResponse), Some(updateConversionStats))
```

**How to spot**
When reviewing core/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17022 fix error-prone in kafkaapis#handlefetchrequest (#16455).

**Test**
No test added.
