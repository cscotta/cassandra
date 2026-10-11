**Commit**: 127a644c7d · **Ticket**: KAFKA-19960
**Subsystem**: streams/src
**Category**: Resource leak

**Root cause**
This PR fixes a bug when KS doesn't close stores if the shutdown was
triggered during rebalance where an active tasks gets converted to a
standby one and put into pendingTasksToInit

* Added logic to close pending tasks to init. * Made standby task closure similar to the one for active tasks. * Added a separate method for getting standby tasks from task registry. * Added an integration test that reproduces the issue.

**Before/After**
```java
// Before
                thread.setStateListener(listener);

// After
                final StreamThread.StateListener originalListener = thread.getStateListener();
                thread.setStateListener((t, newState, oldState) -> {
                    originalListener.onChange(t, newState, oldState);
                    listener.onChange(t, newState, oldState);
                });
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
```

**How to spot**
When reviewing streams/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19960: close pending tasks on shutdown. (#21365).

**Test**
Test modified in KafkaStreamsWrapper.java covering the corrected behaviour.
