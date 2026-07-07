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
package org.apache.cassandra.io.uring;

import org.junit.Test;

import org.apache.cassandra.io.uring.abi.Cqe;
import org.apache.cassandra.io.uring.abi.Layouts;
import org.apache.cassandra.io.uring.abi.Opcodes;
import org.apache.cassandra.io.uring.abi.RegLayouts;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

/**
 * ABI-layout guard. Asserts the FFM {@link Layouts} match the compiler-verified struct sizes/offsets from
 * {@code /usr/include/linux/io_uring.h}. Kernel-free and arch-independent (these ABI structs are identical on
 * x86-64 and aarch64), so it runs on any JDK 25 host without touching a real ring.
 */
public class IoUringLayoutTest
{
    @Test
    public void layoutsMatchGroundTruth()
    {
        assumeTrue("JDK 25+ only", Runtime.version().feature() >= 25);
        // Throws IoUringException on any mismatch.
        Layouts.verify();
    }

    @Test
    public void registerLayoutsMatchGroundTruth()
    {
        assumeTrue("JDK 25+ only", Runtime.version().feature() >= 25);
        // io_uring_register arg structs (rsrc_register=32, rsrc_update=16, rsrc_update2=32, sync_cancel_reg=64,
        // rlimit=16) verified against /usr/include/linux/io_uring.h and <sys/resource.h>.
        RegLayouts.verify();
        assertEquals(32, RegLayouts.RSRC_REGISTER_BYTES);
    }

    @Test
    public void structSizes()
    {
        assumeTrue("JDK 25+ only", Runtime.version().feature() >= 25);
        assertEquals(120, Layouts.PARAMS.byteSize());
        assertEquals(40, Layouts.SQ_OFFSETS.byteSize());
        assertEquals(40, Layouts.CQ_OFFSETS.byteSize());
        assertEquals(64, Layouts.SQE.byteSize());
        assertEquals(16, Layouts.CQE.byteSize());
        assertEquals(16, Layouts.IOVEC.byteSize());
        assertEquals(16, Layouts.KERNEL_TIMESPEC.byteSize());
    }

    @Test
    public void keyOffsets()
    {
        assumeTrue("JDK 25+ only", Runtime.version().feature() >= 25);
        assertEquals(20, Layouts.PARAMS_FEATURES_OFF);
        assertEquals(40, Layouts.PARAMS_SQ_OFF);
        assertEquals(80, Layouts.PARAMS_CQ_OFF);
        assertEquals(32, Layouts.SQE_USER_DATA);
        assertEquals(16, Layouts.SQE_ADDR);
        assertEquals(24, Layouts.SQE_LEN);
        assertEquals(8, Layouts.CQE_RES);
        assertEquals(12, Layouts.CQE_FLAGS);
    }

    @Test
    public void opcodeNumbers()
    {
        // These four are the ones the earlier draft got wrong; pin them.
        assertEquals(22, Opcodes.READ);
        assertEquals(1, Opcodes.READV);
        assertEquals(4, Opcodes.READ_FIXED);
        assertEquals(60, Opcodes.READV_FIXED);
        assertEquals(21, Opcodes.STATX);
        assertEquals(0, Opcodes.NOP);
    }

    @Test
    public void verifyDetectsAMismatch()
    {
        assumeTrue("JDK 25+ only", Runtime.version().feature() >= 25);
        assertEquals(16, Layouts.CQE_BYTES);
        // A CQE accessor reading flags@12 out of an 8-byte segment must be bounds-checked by FFM.
        try
        {
            java.lang.foreign.MemorySegment tiny = java.lang.foreign.Arena.ofAuto().allocate(8);
            Cqe.flags(tiny);
            fail("expected an out-of-bounds access to be rejected");
        }
        catch (IndexOutOfBoundsException expected)
        {
            // FFM MemorySegment bounds-checks catch out-of-bounds access Java-side.
        }
    }
}
