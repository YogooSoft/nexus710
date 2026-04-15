/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.test;

import com.carrotsearch.randomizedtesting.ThreadFilter;

/**
 * Filter out threads that are not controlled by tests but may be created
 * by the JDK, Gradle, or Lucene infrastructure during test execution.
 */
public class EsThreadLeakFilter implements ThreadFilter {

    @Override
    public boolean reject(Thread t) {
        String name = t.getName();

        // Gradle worker threads
        if (name.startsWith("Exec process") || name.startsWith("File watcher consumer")) {
            return true;
        }

        // JDK common ForkJoinPool
        if (name.startsWith("ForkJoinPool.commonPool")) {
            return true;
        }

        // JDK process reaper
        if (name.startsWith("process reaper")) {
            return true;
        }

        // JDK Keep-Alive thread (HttpURLConnection)
        if ("Keep-Alive-Timer".equals(name)) {
            return true;
        }

        // JDK HttpClient threads
        if (name.startsWith("HttpClient-")) {
            return true;
        }

        // Reference Handler and Finalizer are JDK system threads
        if ("Reference Handler".equals(name) || "Finalizer".equals(name)) {
            return true;
        }

        // JDK Signal Dispatcher
        if ("Signal Dispatcher".equals(name)) {
            return true;
        }

        // Notification Thread (JMX, JDK internal)
        if ("Notification Thread".equals(name)) {
            return true;
        }

        // Common Cleaner thread (JDK 9+)
        if ("Common-Cleaner".equals(name)) {
            return true;
        }

        // Attach Listener (debugger / profiler)
        if ("Attach Listener".equals(name)) {
            return true;
        }

        // Gradle daemon threads
        if (name.startsWith("Daemon ") || name.startsWith("Worker ")) {
            return true;
        }

        return false;
    }
}
