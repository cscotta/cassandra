**Commit**: 7a8edffad1 · **Ticket**: KAFKA-17267
**Subsystem**: group-coordinator/src
**Category**: Logic error in condition

**Root cause**
When handling an OFFSET_FETCH request requiring stable offsets, the new
group coordinator may encounter a timeout under some circumstances, such
as a zombie coordinator or a lagging __consumer_offsets replica that has
not yet dropped out of the ISR. Existing and older clients do not expect
the REQUEST_TIMED_OUT error code won't retry, so remap it to
NOT_COORDINATOR to trigger a coordinator lookup and retry. Reviewers: David Jacot <djacot@confluent.io>

**Before/After**
```java
// Before
            );
            );

// After
            ).exceptionally(exception -> handleOffsetFetchException(
                "fetch-offsets",
                request,
                exception
            ));
            ).exceptionally(exception -> handleOffsetFetchException(
                "fetch-all-offsets",
                request,
                exception
            ));

    /**
```

**How to spot**
When reviewing group-coordinator/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17267; don't return request_timed_out for offset_fetches (#16825).

**Test**
Test modified in GroupCoordinatorServiceTest.java covering the corrected behaviour.
