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
package org.apache.cassandra.io.uring.spi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SPI seam for logging, so the extractable core never hard-codes a logging backend. Each level takes either an
 * slf4j-style {@code "{}"} parameterized message plus arguments, or a message plus a {@link Throwable}. The two
 * overloads are unambiguous: a call whose trailing argument is statically a {@code Throwable} binds to the throwable
 * overload, everything else to the varargs overload.
 *
 * <p>The default {@link Slf4jUringLogger} simply forwards to an {@link Logger}; the Cassandra adapter can instead
 * route through its own logging conventions by supplying a different implementation.
 */
public interface UringLogger
{
    /** Log at DEBUG with an slf4j {@code "{}"} parameterized message. */
    void debug(String format, Object... args);

    /** Log at DEBUG with a message and an accompanying throwable. */
    void debug(String message, Throwable t);

    /** Log at INFO with an slf4j {@code "{}"} parameterized message. */
    void info(String format, Object... args);

    /** Log at INFO with a message and an accompanying throwable. */
    void info(String message, Throwable t);

    /** Log at WARN with an slf4j {@code "{}"} parameterized message. */
    void warn(String format, Object... args);

    /** Log at WARN with a message and an accompanying throwable. */
    void warn(String message, Throwable t);

    /** Log at ERROR with an slf4j {@code "{}"} parameterized message. */
    void error(String format, Object... args);

    /** Log at ERROR with a message and an accompanying throwable. */
    void error(String message, Throwable t);

    /**
     * Default implementation forwarding every call verbatim to a backing slf4j {@link Logger}. The parameterized and
     * throwable overloads map one-to-one onto the matching slf4j methods, so message formatting and level filtering
     * are handled entirely by slf4j.
     */
    final class Slf4jUringLogger implements UringLogger
    {
        private final Logger logger;

        public Slf4jUringLogger(Logger logger)
        {
            this.logger = logger;
        }

        /** Convenience factory binding to the slf4j logger named after {@code owner}. */
        public static Slf4jUringLogger forClass(Class<?> owner)
        {
            return new Slf4jUringLogger(LoggerFactory.getLogger(owner));
        }

        @Override
        public void debug(String format, Object... args)
        {
            logger.debug(format, args);
        }

        @Override
        public void debug(String message, Throwable t)
        {
            logger.debug(message, t);
        }

        @Override
        public void info(String format, Object... args)
        {
            logger.info(format, args);
        }

        @Override
        public void info(String message, Throwable t)
        {
            logger.info(message, t);
        }

        @Override
        public void warn(String format, Object... args)
        {
            logger.warn(format, args);
        }

        @Override
        public void warn(String message, Throwable t)
        {
            logger.warn(message, t);
        }

        @Override
        public void error(String format, Object... args)
        {
            logger.error(format, args);
        }

        @Override
        public void error(String message, Throwable t)
        {
            logger.error(message, t);
        }
    }
}
