**Commit**: ac583ad2c0 · **Ticket**: KAFKA-19455
**Subsystem**: server-common/src
**Category**: Logic error in condition

**Root cause**
* If we get an `UNKNOWN_TOPIC_OR_PARTITION` response from the
`ShareCoordinator` is could imply a transient issue where the metadata
image is not upto date. * In this case it makes sense to retry the request to give time for data
to be available. * In this PR, we are making the required change. Reviewers: Andrew Schofield <aschofield@confluent.io>

**Before/After**
```java
// Before
// (no removals)

// After
                case UNKNOWN_TOPIC_OR_PARTITION:
                            case UNKNOWN_TOPIC_OR_PARTITION:
                            case UNKNOWN_TOPIC_OR_PARTITION:
                            case UNKNOWN_TOPIC_OR_PARTITION:
                            case UNKNOWN_TOPIC_OR_PARTITION:
                            case UNKNOWN_TOPIC_OR_PARTITION:
        this.metadataImage = newImage;
```

**How to spot**
When reviewing server-common/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19455: retry persister request on metadata image issues. (#20078).

**Test**
No test added.
