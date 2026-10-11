**Commit**: 050edfaf00 · **Ticket**: KAFKA-14336
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Avoids stream allocation on hot code path in Admin#listOffsets

This patch avoids allocating the stream reference pipeline & spliterator for this case by explicitly allocating the pre-sized Node[] and using a for loop with int induction over the specified IDs List argument. Reviewers: Apoorv Mittal <apoorvmittal10@gmail.com>, Kirk True <kirk@kirktrue.pro>, David Arthur <mumrah@gmail.com>

**Before/After**
```java
// Before
        return replicaIds.stream().map(replicaId -> {
                return new Node(replicaId, "", -1);
            return node;
        }).toArray(Node[]::new);

// After
        // Since this is on hot path for partition info, use indexed iteration to avoid allocation overhead of Streams.
        int size = replicaIds.size();
        Node[] nodes = new Node[size];
        for (int i = 0; i < size; i++) {
            Integer replicaId = replicaIds.get(i);
                node = new Node(replicaId, "", -1);
            nodes[i] = node;
        }
        return nodes;
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-14336: metadataresponse#converttonodearray uses iteration (#12782).

**Test**
No test added.
