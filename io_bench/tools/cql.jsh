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

// cql.jsh -- run ONE CQL statement through the DataStax driver bundled in the
// easy-stress fat jar, using jshell (pure JDK, no cqlsh, no Python driver --
// neither runs on this host's Python 3.14). Invoke via tools/cql.sh, which puts
// the fat jar on --class-path and passes the statement in the CQL env var.
//
// Output is marker-prefixed so the caller can grep it:
//   CQLROW <col>=<val> <col>=<val> ...   (one line per result row)
//   CQLOK norows                         (statement ran, no rows)
//   CQLERR <Exception>: <message>        (connection/statement failure)
//
// Connection facts verified live: contact 127.0.0.1:9042, localDatacenter
// "datacenter1" (SimpleSnitch; "dc1" yields NoNodeAvailableException).

import com.datastax.oss.driver.api.core.*;
import com.datastax.oss.driver.api.core.cql.*;
import java.net.InetSocketAddress;

String host = System.getenv().getOrDefault("CONTACT_HOST", "127.0.0.1");
int port = Integer.parseInt(System.getenv().getOrDefault("CONTACT_PORT", "9042"));
String dc = System.getenv().getOrDefault("LOCAL_DC", "datacenter1");
String cql = System.getenv().getOrDefault("CQL", "");

try (CqlSession session = CqlSession.builder()
        .addContactPoint(new InetSocketAddress(host, port))
        .withLocalDatacenter(dc)
        .build()) {
    if (cql.isEmpty()) {
        System.out.println("CQLERR EmptyStatement: no CQL env var");
    } else {
        ResultSet rs = session.execute(cql);
        ColumnDefinitions defs = rs.getColumnDefinitions();
        boolean any = false;
        for (Row row : rs) {
            any = true;
            StringBuilder sb = new StringBuilder("CQLROW");
            for (int i = 0; i < defs.size(); i++) {
                sb.append(' ')
                  .append(defs.get(i).getName().asInternal())
                  .append('=')
                  .append(String.valueOf(row.getObject(i)));
            }
            System.out.println(sb.toString());
        }
        if (!any) {
            System.out.println("CQLOK norows");
        }
    }
} catch (Exception e) {
    System.out.println("CQLERR " + e.getClass().getSimpleName() + ": " + e.getMessage());
}

/exit
