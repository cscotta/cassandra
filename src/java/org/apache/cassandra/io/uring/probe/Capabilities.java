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
package org.apache.cassandra.io.uring.probe;

import org.apache.cassandra.io.uring.abi.FeatureFlags;

/**
 * Immutable record of what the running kernel offers, captured once by {@link IoUringAvailability}: whether io_uring
 * is usable at all, the {@code IORING_FEAT_*} bitset, and which opcodes the kernel reported as supported (via
 * {@code IORING_REGISTER_PROBE}). When unavailable, {@link #reason()} explains why so the fallback path can log it.
 */
public final class Capabilities
{
    private final boolean available;
    private final int features;
    private final long supportedOpcodes;   // bit i set == opcode i supported
    private final String reason;

    private Capabilities(boolean available, int features, long supportedOpcodes, String reason)
    {
        this.available = available;
        this.features = features;
        this.supportedOpcodes = supportedOpcodes;
        this.reason = reason;
    }

    public static Capabilities available(int features, long supportedOpcodes)
    {
        return new Capabilities(true, features, supportedOpcodes, "available");
    }

    public static Capabilities unavailable(String reason)
    {
        return new Capabilities(false, 0, 0L, reason);
    }

    public boolean isAvailable()
    {
        return available;
    }

    public int features()
    {
        return features;
    }

    public boolean hasFeature(int featureFlag)
    {
        return FeatureFlags.has(features, featureFlag);
    }

    public boolean supports(int opcode)
    {
        if (opcode < 0 || opcode > 63)
            return false;
        return (supportedOpcodes & (1L << opcode)) != 0;
    }

    public String reason()
    {
        return reason;
    }

    @Override
    public String toString()
    {
        if (!available)
            return "Capabilities{unavailable: " + reason + '}';
        return "Capabilities{available, features=0x" + Integer.toHexString(features)
               + ", supportedOpcodes=0x" + Long.toHexString(supportedOpcodes) + '}';
    }
}
