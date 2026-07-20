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

// PersistentReader -- a long-lived single-partition read client for Phase A.
//
// Run as a JDK single-file source program (no separate compile step) against the
// DataStax driver bundled in the cassandra-easy-stress fat jar:
//
//   java --class-path <easy-stress-all.jar> --source 25 \
//        io_bench/tools/PersistentReader.java <host> <port> <dc> <ks> <table>
//
// It connects ONCE, prepares SELECT * FROM <table> WHERE key = ?, prints "READY",
// then loops: read one key per stdin line, execute exactly that one read, print
// "DONE key=<k> rows=<n> us=<micros>". "QUIT" (or EOF) exits with "BYE".
//
// Why: Phase A spawning a fresh easy-stress JVM per read paid a full cold driver
// connect + schema-metadata fetch every time (system.local + system_schema.*),
// which dwarfed the one query in the eBPF device window. A persistent, already-
// warm client means each measured window contains ONLY the single read's IO.
// All driver/SLF4J logging goes to stderr; stdout carries only the markers.

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.Row;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;

public class PersistentReader
{
    public static void main(String[] args) throws Exception
    {
        String host = args.length > 0 ? args[0] : "127.0.0.1";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 9042;
        String dc = args.length > 2 ? args[2] : "datacenter1";
        String ks = args.length > 3 ? args[3] : "iobench_ks";
        String table = args.length > 4 ? args[4] : "keyvalue";

        try (CqlSession session = CqlSession.builder()
                                            .addContactPoint(new InetSocketAddress(host, port))
                                            .withLocalDatacenter(dc)
                                            .withKeyspace(ks)
                                            .build())
        {
            PreparedStatement select = session.prepare("SELECT * FROM " + table + " WHERE " + "key = ?");
            System.out.println("READY");
            System.out.flush();

            BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
            String line;
            while ((line = in.readLine()) != null)
            {
                line = line.trim();
                if (line.isEmpty())
                    continue;
                if (line.equals("QUIT"))
                    break;
                long t0 = System.nanoTime();
                int rows = 0;
                for (Row r : session.execute(select.bind(line)))
                {
                    rows++;
                    r.getColumnDefinitions(); // touch the row so nothing is optimised away
                }
                long micros = (System.nanoTime() - t0) / 1000L;
                System.out.println("DONE key=" + line + " rows=" + rows + " us=" + micros);
                System.out.flush();
            }
        }
        System.out.println("BYE");
        System.out.flush();
    }
}
