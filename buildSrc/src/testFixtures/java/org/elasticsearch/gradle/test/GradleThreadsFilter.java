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

package org.elasticsearch.gradle.test;

import com.carrotsearch.randomizedtesting.ThreadFilter;

/**
 * Filter out threads controlled by Gradle or the JDK that may be created during unit tests.
 *
 * This includes pooled threads for Exec, file system watcher threads, and auxiliary background
 * threads observed from recent Gradle/JDK combinations on macOS.
 */
public class GradleThreadsFilter implements ThreadFilter {

    @Override
    public boolean reject(Thread t) {
        String threadName = t.getName();
        if (threadName.startsWith("Exec process") || threadName.startsWith("File watcher consumer")) {
            return true;
        }
        if (threadName.startsWith("sshd-SshClient")) {
            return true;
        }
        if ("Memory manager".equals(threadName)) {
            return true;
        }
        for (StackTraceElement element : t.getStackTrace()) {
            if ("sun.nio.ch.KQueuePort$EventHandlerTask".equals(element.getClassName())) {
                return true;
            }
        }
        return false;
    }
}
