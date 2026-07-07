/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.cassandra.io.uring.cassandra;

import java.util.concurrent.CompletableFuture; // checkstyle: permit this import
import java.util.concurrent.ExecutionException;

/**
 * Awaits an io_uring read completion without ever abandoning the DMA-target buffer while the kernel may still be
 * writing into it. A blocking {@code get()} that threw {@link InterruptedException} would return with the op still in
 * flight and its buffer live; the caller would then recycle that buffer and a later completion would corrupt whatever
 * next occupies the region &mdash; a use-after-recycle the synchronous {@code FileChannel} path cannot hit. So on
 * interrupt we record it and keep waiting for the op's terminal completion (the ring's bounded drain guarantees the
 * future completes even on teardown), restoring the thread's interrupt status before returning. Shared by the
 * uncompressed {@link IoUringChunkReader} and the compressed {@link IoUringFrameReader}.
 */
final class UringAwait
{
    private UringAwait() {}

    static int uninterruptibly(CompletableFuture<Integer> future) throws ExecutionException
    {
        boolean interrupted = false;
        try
        {
            while (true)
            {
                try
                {
                    return future.get();
                }
                catch (InterruptedException e)
                {
                    interrupted = true;
                }
            }
        }
        finally
        {
            if (interrupted)
                Thread.currentThread().interrupt();
        }
    }
}
