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
package org.apache.cassandra.io.uring.ring;

/**
 * Callback for reaping one completion. Receives the decoded {@code io_uring_cqe} fields directly (no per-CQE
 * allocation): the {@code user_data} correlation token, {@code res} (bytes transferred if {@code >= 0}, else
 * {@code -errno}; {@code 0} is EOF for reads), and the {@code IORING_CQE_F_*} flags.
 */
@FunctionalInterface
public interface CqeConsumer
{
    void accept(long userData, int res, int flags);
}
