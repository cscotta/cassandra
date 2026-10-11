**Commit**: dfb178a1d8 · **Ticket**: KAFKA-18272
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
This makes it possible to enable request logs for deprecated protocol api versions without enabling it for the rest. Combined with the ability to enable/disable dynamically, it makes it a bit easier to collect the information about deprecated clients that is not available via metrics. This isn't particularly useful in trunk/4.0 since there are no deprecated api versions in these versions, but it will be useful for older branches. I intend to backport to those branches and add a release note in the backport regarding the change in behavior.

**Before/After**
```java
// Before
  private def isRequestLoggingEnabled: Boolean = requestLogger.underlying.isDebugEnabled
      if (RequestChannel.isRequestLoggingEnabled) Some(RequestConvertToJson.request(loggableRequest))
      if (RequestChannel.isRequestLoggingEnabled)
      if (isRequestLoggingEnabled) {
        requestLogger.debug("Completed request:" + desc.toString)
        if (header.apiKey().isVersionDeprecated(header.apiVersion())) {

// After
    public boolean isApiVersionDeprecated() {
        return apiKey().isVersionDeprecated(apiVersion());
    }

  /**
    * Deprecated protocol apis are logged at info level while the rest are logged at debug level.
    * That makes it possible to enable the former without enabling latter.
    */
  private def isRequestLoggingEnabled(header: RequestHeader): Boolean = requestLogger.underlying.isDebugEnabled ||
    (requestLogger.underlying.isInfoEnabled && header.isApiVersionDeprecated())
      if (RequestChannel.isRequestLoggingEnabled(context.header)) Some(RequestConvertToJson.request(loggableRequest))
      if (RequestChannel.isRequestLoggingEnabled(context.header))
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18272: deprecated protocol api usage should be logged at info level (#18313).

**Test**
No test added.
