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

import org.apache.cassandra.io.uring.abi.FeatureFlags;

/**
 * Immutable geometry and capabilities of a live ring, captured from {@code io_uring_params} at setup: the actual
 * SQ/CQ sizes the kernel granted (may exceed the requested {@code entries}, rounded up to a power of two), the
 * {@code IORING_FEAT_*} bitset, the {@code IORING_SETUP_*} flags used, and whether the SQ and CQ share one mmap.
 */
public final class RingParams
{
    private final int sqEntries;
    private final int cqEntries;
    private final int features;
    private final int setupFlags;
    private final boolean singleMmap;

    public RingParams(int sqEntries, int cqEntries, int features, int setupFlags, boolean singleMmap)
    {
        this.sqEntries = sqEntries;
        this.cqEntries = cqEntries;
        this.features = features;
        this.setupFlags = setupFlags;
        this.singleMmap = singleMmap;
    }

    public int sqEntries()
    {
        return sqEntries;
    }

    public int cqEntries()
    {
        return cqEntries;
    }

    public int features()
    {
        return features;
    }

    public int setupFlags()
    {
        return setupFlags;
    }

    public boolean singleMmap()
    {
        return singleMmap;
    }

    public boolean hasFeature(int featureFlag)
    {
        return FeatureFlags.has(features, featureFlag);
    }

    @Override
    public String toString()
    {
        return "RingParams{sqEntries=" + sqEntries + ", cqEntries=" + cqEntries
               + ", features=0x" + Integer.toHexString(features)
               + ", setupFlags=0x" + Integer.toHexString(setupFlags)
               + ", singleMmap=" + singleMmap + '}';
    }
}
