**Commit**: 17846fe743 · **Ticket**: KAFKA-16372
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Currently, Producer.send doc is inconsistent with actual exception behavior
      - TimeoutException: This won't be thrown from send on buffer-full or metadata-missing actually. Instead, it will returned as failed future. - AuthenticationException/AuthorizationException: These exceptions are also won't be thrown. Returned with failed future actually.

**Before/After**
```java
// Before
 * exhausted additional send calls will block. The threshold for time to block is determined by <code>max.block.ms</code> after which it throws
 * a TimeoutException.
     * @throws AuthenticationException if authentication fails. See the exception for more details
     * @throws AuthorizationException fatal error indicating that the producer is not allowed to write
     * @throws TimeoutException If the record could not be appended to the send buffer due to memory unavailable
     *                          or missing metadata within {@code max.block.ms}.
                                                    + "sent faster than they can be delivered to the server the producer will block for <code>" + MAX_BLOCK_MS_CONFIG + "</code> aft

// After
     *                      <li>{@link org.apache.kafka.common.errors.AuthenticationException AuthenticationException}
     *                      <li>{@link org.apache.kafka.common.errors.AuthorizationException AuthorizationException}
     *                      <li>{@link org.apache.kafka.clients.producer.BufferExhaustedException BufferExhaustedException}
 * exhausted additional send calls will block. The threshold for time to block is determined by <code>max.block.ms</code> after which it returns
 * a failed future with BufferExhaustedException.
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16372 fix producer doc discrepancy with the exception behavior (#15574).

**Test**
No test added.
