**Commit**: 836f52b0ba · **Ticket**: KAFKA-16000
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Finishing migration of MembershipManagerImplTest away from ConsumerTestBuilder and removed all spy objects. Reviewers: Lianet Magrans <lianetmr@gmail.com>, Philip Nee <pnee@confluent.io>, Matthias J. Sax <matthias@confluent.io>

**Before/After**
```java
// Before
import org.junit.jupiter.api.AfterEach;
    private final LogContext logContext = new LogContext();


    private ConsumerTestBuilder testBuilder;
        testBuilder = new ConsumerTestBuilder(ConsumerTestBuilder.createDefaultGroupInformation());
        metadata = testBuilder.metadata;
        subscriptionState = testBuilder.subscriptions;
        commitRequestManager = testBuilder.commitRequestManager.orElseThrow(IllegalStateException::new);
        backgroundEventQueue = testBuilder.backgroundEventQueue;
        backgroundEventHandler = testBuilder.backgroundEventHandler;
    }

// After
import java.util.concurrent.LinkedBlockingQueue;
import static org.apache.kafka.clients.consumer.internals.MembershipManagerImpl.TOPIC_PARTITION_COMPARATOR;
import static org.mockito.ArgumentMatchers.eq;
    private static final LogContext LOG_CONTEXT = new LogContext();
        metadata = mock(ConsumerMetadata.class);
        subscriptionState = mock(SubscriptionState.class);
        commitRequestManager = mock(CommitRequestManager.class);
        backgroundEventQueue = new LinkedBlockingQueue<>();
        backgroundEventHandler = new BackgroundEventHandler(backgroundEventQueue);
        when(commitRequestManager.maybeAutoCommitSyncBeforeRevocation(anyLong())).thenReturn(CompletableFuture.completedFuture(null));
            subscriptionState, commitRequestManager, metadata, LOG_CONTEXT, Optional.empty(),
                metadata, LOG_CONTEXT, Optional.empty(), backgroundEventHandler, time,
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16000: migrated membershipmanagerimpltest away from consumertestbuilder (#16312).

**Test**
Test modified in MembershipManagerImplTest.java covering the corrected behaviour.
