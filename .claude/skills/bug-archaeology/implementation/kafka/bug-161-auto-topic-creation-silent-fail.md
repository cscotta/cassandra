**Commit**: 3067f15caf · **Ticket**: KAFKA-19596
**Subsystem**: core/src/main/scala/kafka/server/AutoTopicCreationManager.scala
**Category**: Silent skip on non-match

**Root cause**
When automatic topic creation failed (e.g., due to `TOPIC_AUTHORIZATION_FAILED` or `INVALID_TOPIC_EXCEPTION`), the `DefaultAutoTopicCreationManager` logged only at `debug` level. At the default log level this meant the failure was completely invisible to operators: the broker showed that it was attempting auto-creation but gave no indication that the attempt had failed or why. The fix adds a `warn`-level log for each failed topic, including the Kafka error name and the error message from the `CreateTopicsResponse`.

**Before/After**
```scala
// Before: only debug log on any completion (success or failure)
debug(s"Auto topic creation completed for ${creatableTopics.keys} with response ${response.responseBody}.")

// After: warn for each failed topic, then debug for full completion
if (response.hasResponse) {
  response.responseBody() match {
    case createTopicsResponse: CreateTopicsResponse =>
      createTopicsResponse.data().topics().forEach(topicResult => {
        val error = Errors.forCode(topicResult.errorCode)
        if (error != Errors.NONE) {
          warn(s"Auto topic creation failed for ${topicResult.name} with error '${error.name}': ${topicResult.errorMessage}")
        }
      })
    case other =>
      warn(s"Auto topic creation request received unexpected response type: ...")
  }
}
debug(s"Auto topic creation completed ...")
```

**How to spot**
Search for fire-and-forget async operations (like `sendRequest` callbacks) where the success path is logged but the failure path only calls `debug`. Any place where an admin operation can fail without emitting at least a `warn` will hide operational problems from users running at default log levels.

**Test**
No test added. This is a visibility improvement that does not change functional behaviour.
