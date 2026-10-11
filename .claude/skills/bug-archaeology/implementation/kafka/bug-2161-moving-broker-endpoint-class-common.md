**Commit**: 25a3590dc2 · **Ticket**: KAFKA-17813
**Subsystem**: clients/src
**Category**: Resource leak

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>, Kuan-Po Tseng <brandboat@gmail.com>, Jun Rao <junrao@gmail.com>

**Before/After**
```java
// Before
    /**
     * Generates a unique connection ID for the given socket.
     *
     * @param socket The socket for which the connection ID is to be generated.
     * @param processorId The ID of the server processor that will handle this connection.
     * @param connectionIndex The index to be used in the connection ID to ensure uniqueness.
     * @return A string representing the unique connection ID.
     */
    public static String generateConnectionId(Socket socket, int processorId, int connectionIndex) {
        String localHost = socket.getLocalAddress().getHostAddress();
        int localPort = socket.getLocalPort();
        String remoteHost = socket.getInetAddress().getHostAddress();

// After
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
```

**How to spot**
When reviewing clients/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17813: moving broker endpoint class and common server connection id (#17519).

**Test**
Test modified in NioEchoServer.java covering the corrected behaviour.
