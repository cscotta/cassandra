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

package org.apache.cassandra.service.reads;

import java.lang.reflect.Field;
import java.util.Map;

import org.junit.Test;

import org.apache.cassandra.db.ConsistencyLevel;
import org.apache.cassandra.db.ReadCommand;
import org.apache.cassandra.db.SimpleBuilders;
import org.apache.cassandra.db.SinglePartitionReadCommand;
import org.apache.cassandra.db.partitions.PartitionUpdate;
import org.apache.cassandra.db.rows.Row;
import org.apache.cassandra.exceptions.ReadFailureException;
import org.apache.cassandra.exceptions.RequestFailure;
import org.apache.cassandra.exceptions.RequestFailureReason;
import org.apache.cassandra.locator.EndpointsForToken;
import org.apache.cassandra.locator.InetAddressAndPort;
import org.apache.cassandra.locator.ReplicaPlan;
import org.apache.cassandra.tcm.Epoch;
import org.apache.cassandra.transport.Dispatcher;

import static org.apache.cassandra.locator.ReplicaUtils.full;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * The read callback keeps the failure reasons reported by the replicas. That map is only needed when a replica
 * fails, so it is created on first use: this asserts the map is absent on the happy path and that a failure is
 * still recorded (and reaches the failure reporting) when one does occur.
 */
public class ReadCallbackFailureReasonsTest extends AbstractReadResponseTest
{
    private static PartitionUpdate responseData()
    {
        SimpleBuilders.RowBuilder builder = new SimpleBuilders.RowBuilder(cfm, "4");
        builder.timestamp(1000).add("c1", "4");
        Row row = builder.build();
        PartitionUpdate.Builder update = new PartitionUpdate.Builder(cfm, dk, cfm.regularAndStaticColumns(), 1, false);
        update.add(row);
        return update.build();
    }

    /**
     * Reads the private field: the point of the change is that the object is not allocated, so there is no
     * behaviour to observe other than the absence of the map.
     */
    private static Map<?, ?> failureReasons(ReadCallback<?, ?> callback) throws Exception
    {
        Field field = ReadCallback.class.getDeclaredField("failureReasonByEndpoint");
        field.setAccessible(true);
        return (Map<?, ?>) field.get(callback);
    }

    private static ReplicaPlan.SharedForTokenRead plan(ConsistencyLevel consistencyLevel, EndpointsForToken replicas)
    {
        return ReplicaPlan.shared(new ReplicaPlan.ForTokenRead(ks, ks.getReplicationStrategy(), consistencyLevel,
                                                              replicas, replicas, replicas, null, (self) -> null,
                                                              Epoch.EMPTY));
    }

    private static ReadCallback<EndpointsForToken, ReplicaPlan.ForTokenRead> callback(ReadCommand command,
                                                                                     EndpointsForToken replicas,
                                                                                     Dispatcher.RequestTime requestTime)
    {
        ReplicaPlan.SharedForTokenRead plan = plan(ConsistencyLevel.QUORUM, replicas);
        DigestResolver<EndpointsForToken, ReplicaPlan.ForTokenRead> resolver
            = new DigestResolver<>(ReadCoordinator.DEFAULT, command, plan, requestTime);
        return new ReadCallback<>(resolver, command, plan, requestTime);
    }

    @Test
    public void failureReasonsAreOnlyTrackedWhenAReplicaFails() throws Exception
    {
        SinglePartitionReadCommand command = SinglePartitionReadCommand.fullPartitionRead(cfm, nowInSec, dk);
        EndpointsForToken replicas = EndpointsForToken.of(dk.getToken(), full(EP1), full(EP2));
        ReadCallback<EndpointsForToken, ReplicaPlan.ForTokenRead> callback
            = callback(command, replicas, new Dispatcher.RequestTime(0L, 0L));

        assertNull("a read that has not failed must not allocate the failure reason map",
                   failureReasons(callback));

        // a response from a replica leaves the map unallocated too
        callback.onResponse(response(command, EP1, iter(responseData()), true));
        assertNull("a response that reports no failure must not allocate the failure reason map",
                   failureReasons(callback));

        callback.onFailure(EP2, RequestFailure.READ_TOO_MANY_TOMBSTONES);

        Map<?, ?> reasons = failureReasons(callback);
        assertNotNull("a reported failure allocates the failure reason map", reasons);
        assertEquals(1, reasons.size());
        assertEquals(RequestFailureReason.READ_TOO_MANY_TOMBSTONES, reasons.get(EP2));
        assertNull("only the failing replica is recorded", reasons.get((InetAddressAndPort) EP1));
    }

    /**
     * The lazily created map must be the one that reaches the client: a read that fails for a reason other than a
     * timeout reports a failure carrying that reason, not a timeout.
     */
    @Test
    public void aReportedFailureReachesTheClientEvenWhenTheMapIsCreatedLate()
    {
        SinglePartitionReadCommand command = SinglePartitionReadCommand.fullPartitionRead(cfm, nowInSec, dk);
        EndpointsForToken replicas = EndpointsForToken.of(dk.getToken(), full(EP1), full(EP2));
        ReadCallback<EndpointsForToken, ReplicaPlan.ForTokenRead> callback
            = callback(command, replicas, new Dispatcher.RequestTime(0L, 0L));

        // one replica answers with data, the other fails: QUORUM of two is not reached
        callback.onResponse(response(command, EP1, iter(responseData()), true));
        callback.onFailure(EP2, RequestFailure.READ_TOO_MANY_TOMBSTONES);

        ReadFailureException failure;
        try
        {
            callback.awaitResults();
            fail("a read that lost a replica to a non-timeout failure must report a failure");
            return;
        }
        catch (ReadFailureException e)
        {
            failure = e;
        }

        assertEquals(1, failure.failureReasonByEndpoint.size());
        assertEquals(RequestFailureReason.READ_TOO_MANY_TOMBSTONES, failure.failureReasonByEndpoint.get(EP2));
        assertEquals(1, failure.received);
        assertEquals(2, failure.blockFor);
        assertTrue("the data replica answered, so the failure reports data as present", failure.dataPresent);
    }
}
