## Commit

- Hash: a116753cc8
- Tickets: KAFKA-17986
- Subject: KAFKA-17986: Re-enable rebootstrap tests (remove @Disabled annotations)

## Subsystem

core / integration tests / rebootstrap

## Category

State not cleaned up

## Root cause

`ConsumerRebootstrapTest`, `ProducerRebootstrapTest`, and `RebootstrapTest` were disabled with `@Disabled("KAFKA-17986")` because the tests had a race condition in topic setup: the test tried to start producing/consuming before unclean leader election was fully configured. The fix moves topic creation with the required unclean-leader-election settings into `@BeforeEach`, guaranteeing the topic is ready before the first test method runs, and re-enables all tests.

## Before

```java
@Disabled("KAFKA-17986")
@Test
void testConsumerRebootstrap() { ... }
```

## After

```java
@BeforeEach
void setUp() {
    // createTopic with unclean.leader.election.enable=true
}

@Test
void testConsumerRebootstrap() { ... }
```

## How to spot

`@Disabled` annotations referencing a JIRA ticket where the underlying issue was a test setup race (topic/broker not ready) rather than a genuine product bug — these should be fixed in `@BeforeEach` and re-enabled rather than left disabled indefinitely.

## Test

All three rebootstrap test classes pass reliably in CI after re-enablement.
