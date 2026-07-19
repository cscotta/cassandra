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

package org.apache.cassandra.io.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A dedicated, opt-in log of every IO operation the database issues, categorized by source.
 *
 * Output is routed to a separate {@code io_operations.log} appender by the {@code io_operations} logback logger,
 * which is at level {@code OFF} by default; enable it (level {@code TRACE}) only for a controlled measurement run.
 * The single gate is {@link #isEnabled()} = {@code logger.isTraceEnabled()}, so when the appender is off the call
 * sites do no work beyond one cached level check.
 *
 * The IO source (read / flush / compaction / streaming / commitlog) is derived from the issuing thread's name,
 * which mirrors the per-{@code comm} breakdown the eBPF collectors produce, so the in-process log and the
 * kernel-side trace can be reconciled on both source and (path, offset, len).
 */
public final class IoOperationsLog
{
    private static final Logger logger = LoggerFactory.getLogger("io_operations");

    public enum IoSource
    {
        READ, FLUSH, COMPACTION, STREAMING, COMMITLOG, UNKNOWN
    }

    private IoOperationsLog()
    {
    }

    /** True only when the io_operations appender is enabled (level TRACE). Gate every call site with this. */
    public static boolean isEnabled()
    {
        return logger.isTraceEnabled();
    }

    public static void logRead(String path, long offset, long length)
    {
        log('r', path, offset, length);
    }

    public static void logWrite(String path, long offset, long length)
    {
        log('w', path, offset, length);
    }

    private static void log(char op, String path, long offset, long length)
    {
        String comp = component(path);
        String ks = keyspaceOf(path);
        String tbl = tableOf(path);
        // logback's encoder prepends the timestamp and issuing [thread]; keep the payload to the IO fields.
        logger.trace("src={} op={} ks={} tbl={} comp={} off={} len={} path={}",
                     sourceForThread(), op, ks, tbl, comp, offset, length, path);
    }

    static IoSource sourceForThread()
    {
        String t = Thread.currentThread().getName();
        if (t == null)
            return IoSource.UNKNOWN;
        // ReadStage-N (SEP worker, renamed when set_sep_thread_name=true) and Native-Transport-Requests-N
        // (a coordinator-local read run inline via Stage.READ.maybeExecuteImmediately) both serve read-path IO.
        if (t.contains("ReadStage") || t.startsWith("Read-") || t.contains("Native-Transport"))
            return IoSource.READ;
        if (t.contains("CompactionExecutor"))
            return IoSource.COMPACTION;
        if (t.contains("MemtableFlushWriter") || t.contains("PerDiskMemtableFlush") || t.contains("MemtablePostFlush"))
            return IoSource.FLUSH;
        if (t.contains("Stream"))
            return IoSource.STREAMING;
        if (t.contains("COMMIT-LOG"))
            return IoSource.COMMITLOG;
        return IoSource.UNKNOWN;
    }

    /** The SSTable component (e.g. {@code Data.db}, {@code Index.db}) inferred from the file name, or "" if unknown. */
    static String component(String path)
    {
        if (path == null)
            return "";
        int slash = path.lastIndexOf('/');
        String name = slash >= 0 ? path.substring(slash + 1) : path;
        int dash = name.lastIndexOf('-');
        return dash >= 0 ? name.substring(dash + 1) : name;
    }

    /** The keyspace directory name from a {@code .../<ks>/<table>-<id>/<file>} path, or "" if it can't be parsed. */
    static String keyspaceOf(String path)
    {
        if (path == null)
            return "";
        int fileSlash = path.lastIndexOf('/');
        int tblSlash = fileSlash > 0 ? path.lastIndexOf('/', fileSlash - 1) : -1;
        int ksSlash = tblSlash > 0 ? path.lastIndexOf('/', tblSlash - 1) : -1;
        return (ksSlash >= 0 && tblSlash > ksSlash) ? path.substring(ksSlash + 1, tblSlash) : "";
    }

    /** The table name (before the {@code -<tableId>} suffix) from the same path shape, or "" if it can't be parsed. */
    static String tableOf(String path)
    {
        if (path == null)
            return "";
        int fileSlash = path.lastIndexOf('/');
        int tblSlash = fileSlash > 0 ? path.lastIndexOf('/', fileSlash - 1) : -1;
        if (tblSlash < 0 || fileSlash <= tblSlash)
            return "";
        String tblDir = path.substring(tblSlash + 1, fileSlash);
        int dash = tblDir.indexOf('-'); // table names cannot contain '-', so the first '-' begins the table id
        return dash > 0 ? tblDir.substring(0, dash) : tblDir;
    }
}
