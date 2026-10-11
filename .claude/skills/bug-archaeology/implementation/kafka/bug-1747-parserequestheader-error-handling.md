**Commit**: 3654bc4513 · **Ticket**: KAFKA-18339
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
A minor refactoring just before merging #18295 introduced a regression and no test failed. Throw the correct exception and add test to verify it. Also refactor the code slightly to make that possible. Thanks to Chia-Ping for catching the issue.

**Before/After**
```java
// Before
        return header.apiKey() == API_VERSIONS && !API_VERSIONS.isVersionSupported(header.apiVersion());
            short version = header.apiVersion();
            if (!apiKey.isVersionSupported(version)) {
  private def parseRequestHeader(buffer: ByteBuffer): RequestHeader = {
    val header = RequestHeader.parse(buffer)
    if (apiVersionManager.isApiEnabled(header.apiKey, header.apiVersion)) {
      header
    } else if (header.isApiVersionDeprecated()) {
      throw new InvalidRequestException(s"Received request api key ${header.apiKey} with version ${header.apiVersion} which is not enabled")
    } else {
      throw new UnsupportedVersionException(s"Received request api key ${header.apiKey} with version ${header.apiVersion} which is not supported")
    }

// After
        return header.apiKey() == API_VERSIONS && !header.isApiVersionSupported();
    public boolean isApiVersionSupported() {
        return apiKey().isVersionSupported(apiVersion());
    }

            short version = header.apiVersion();
            if (!header.isApiVersionSupported()) {

  private[network] def parseRequestHeader(apiVersionManager: ApiVersionManager, buffer: ByteBuffer): RequestHeader = {
    val header = RequestHeader.parse(buffer)
    if (apiVersionManager.isApiEnabled(header.apiKey, header.apiVersion)) {
      header
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18339: fix parserequestheader error handling (#18340).

**Test**
Test modified in ProcessorTest.scala covering the corrected behaviour.
