**Commit**: eafa78d99d · **Ticket**: KAFKA-18016
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
What
There was a bug in handling piggyback acknowledgements in ShareConsumeRequestManager, where the fetchAcknowledgementsMap could be updated when the request was in flight and when the ShareFetch response is received, we were removing any acknowledgements(without actually sending them) which came when the request was in flight. Fix
Now we are maintaining 2 separate maps(one which has the acknowledgements to send and one which keeps track of the acknowledgements in flight). Reviewers: Andrew Schofield <aschofield@confluent.io>, Apoorv Mittal <apoorvmittal10@gmail.com>,  Manikumar Reddy <manikumar.reddy@gmail.com>

**Before/After**
```java
// Before
    private final Map<TopicIdPartition, Acknowledgements> fetchAcknowledgementsMap;
    private final Map<IdAndPartition, String> forgottenTopicNames = new HashMap<>();
        this.fetchAcknowledgementsMap = new HashMap<>();
                Acknowledgements acknowledgementsToSend = fetchAcknowledgementsMap.get(tip);
                            Acknowledgements acknowledgementsToSend = fetchAcknowledgementsMap.get(tip);
                            forgottenTopicNames.putIfAbsent(new IdAndPartition(tip.topicId(), tip.partition()), tip.topic());
        acknowledgementsMap.forEach((tip, acks) -> fetchAcknowledgementsMap.merge(tip, acks, Acknowledgements::merge));

// After
    private final Map<TopicIdPartition, Acknowledgements> fetchAcknowledgementsToSend;
    private final Map<TopicIdPartition, Acknowledgements> fetchAcknowledgementsInFlight;
    private final Map<IdAndPartition, String> topicNamesMap = new HashMap<>();
        this.fetchAcknowledgementsToSend = new HashMap<>();
        this.fetchAcknowledgementsInFlight = new HashMap<>();
                Acknowledgements acknowledgementsToSend = fetchAcknowledgementsToSend.remove(tip);
                    fetchAcknowledgementsInFlight.put(tip, acknowledgementsToSend);
                topicNamesMap.putIfAbsent(new IdAndPartition(tip.topicId(), tip.partition()), tip.topic());
                            Acknowledgements acknowledgementsToSend = fetchAcknowledgementsToSend.remove(tip);

                                fetchAcknowledgementsInFlight.put(tip, acknowledgementsToSend);

```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18016: modified handling of piggyback acknowledgements in shareconsumerequestmanager. (#17824).

**Test**
Test modified in ShareConsumeRequestManagerTest.java covering the corrected behaviour.
