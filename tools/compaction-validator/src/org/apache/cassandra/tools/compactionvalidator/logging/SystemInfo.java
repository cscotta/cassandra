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
package org.apache.cassandra.tools.compactionvalidator.logging;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Collects system information at startup for inclusion in log file headers.
 */
public final class SystemInfo
{
    private SystemInfo()
    {
    }

    /**
     * Returns the number of logical CPU cores available to the JVM.
     */
    public static int cpuCores()
    {
        return Runtime.getRuntime().availableProcessors();
    }

    /**
     * Returns the total physical memory in bytes if the
     * {@code com.sun.management.OperatingSystemMXBean} is available, otherwise
     * falls back to {@code Runtime.getRuntime().maxMemory()}.
     */
    public static long totalMemoryBytes()
    {
        try
        {
            Object bean = ManagementFactory.getOperatingSystemMXBean();
            if (bean instanceof com.sun.management.OperatingSystemMXBean)
            {
                long physical = ((com.sun.management.OperatingSystemMXBean) bean).getTotalPhysicalMemorySize();
                if (physical > 0)
                    return physical;
            }
        }
        catch (Throwable ignored)
        {
            // Fall through to the Runtime fallback below.
        }
        return Runtime.getRuntime().maxMemory();
    }

    /**
     * Returns the OS name and version as a single string.
     */
    public static String osVersion()
    {
        return System.getProperty("os.name") + " " + System.getProperty("os.version");
    }

    /**
     * Returns the JVM vendor and version as a single string.
     */
    public static String jvmVersion()
    {
        return System.getProperty("java.vendor") + " " + System.getProperty("java.version");
    }

    /**
     * Returns the local hostname, or {@code "unknown"} if it cannot be resolved.
     */
    public static String hostname()
    {
        try
        {
            return InetAddress.getLocalHost().getHostName();
        }
        catch (UnknownHostException e)
        {
            return "unknown";
        }
    }

    /**
     * Returns a multi-line summary suitable for a log file header, including all
     * system information fields formatted for readability.
     */
    public static String summary()
    {
        long totalBytes = totalMemoryBytes();
        long totalMiB = totalBytes / (1024L * 1024L);

        return "  Host:    " + hostname() + "\n" +
               "  OS:      " + osVersion() + "\n" +
               "  JVM:     " + jvmVersion() + "\n" +
               "  CPU:     " + cpuCores() + " cores\n" +
               "  Memory:  " + totalMiB + " MiB";
    }
}
