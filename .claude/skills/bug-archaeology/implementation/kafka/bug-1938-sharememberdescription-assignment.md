**Commit**: e7bbcdb251 · **Ticket**: KAFKA-18090
**Subsystem**: clients/src
**Category**: Resource leak

**Root cause**
Introduce ShareMemberDescription and ShareMemberAssignment as distinct classes for share groups. Although the correspondence with consumer groups is fairly close, the concepts are likely to diverge over time and separating these concepts now makes sense. Reviewers: Manikumar Reddy <manikumar.reddy@gmail.com>

**Before/After**
```java
// Before
 * A detailed description of a single group instance in the cluster.
    private final Collection<MemberDescription> members;
                                 Collection<MemberDescription> members,
                                 Collection<MemberDescription> members,
    public Collection<MemberDescription> members() {
            ", members=" + members.stream().map(MemberDescription::toString).collect(Collectors.joining(",")) +

// After
 * A detailed description of a single group member in the cluster.
    private final Collection<ShareMemberDescription> members;
                                 Collection<ShareMemberDescription> members,
                                 Collection<ShareMemberDescription> members,
    public Collection<ShareMemberDescription> members() {
            ", members=" + members.stream().map(ShareMemberDescription::toString).collect(Collectors.joining(",")) +
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
```

**How to spot**
When reviewing clients/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18090: add sharememberdescription and assignment (#17975).

**Test**
Test modified in KafkaAdminClientTest.java covering the corrected behaviour.
