**Commit**: 2bb9f53477 · **Ticket**: KAFKA-17465
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Reviewers: TengYao Chi <kitingiao@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
    private List<MemberIdentity> getMembersFromGroup(String groupId, String reason) {
        Collection<MemberDescription> members;
        try {
            members = describeConsumerGroups(Collections.singleton(groupId)).describedGroups().get(groupId).get().members();
        } catch (Exception ex) {
            throw new KafkaException("Encounter exception when trying to get members from group: " + groupId, ex);
        }
        List<MemberIdentity> membersToRemove = new ArrayList<>();
        for (final MemberDescription member : members) {
            MemberIdentity memberIdentity = new MemberIdentity().setReason(reason);

            if (member.groupInstanceId().isPresent()) {

// After
    private KafkaFutureImpl<List<MemberIdentity>> getMembersFromGroup(String groupId, String reason) {
        KafkaFutureImpl<List<MemberIdentity>> future = new KafkaFutureImpl<>();
        describeConsumerGroups(Collections.singleton(groupId)).describedGroups().get(groupId).whenComplete((res, ex) -> {
            if (ex != null) {
                future.completeExceptionally(new KafkaException("Encounter exception when trying to get members from group: " + groupId, ex));
                List<MemberIdentity> membersToRemove = res.members().stream().map(member ->
                    member.groupInstanceId().map(id -> new MemberIdentity().setGroupInstanceId(id))
                    .orElseGet(() -> new MemberIdentity().setMemberId(member.consumerId()))
                    .setReason(reason)
                ).collect(Collectors.toList());

                future.complete(membersToRemove);
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17465 refactor getmembersfromgroup to be non-blocking (#17080).

**Test**
No test added.
