/*
 * Licensed under the Apache License, Version 2.0
 * Based on Elasticsearch 7.10.2 (Apache 2.0)
 */

package io.nexus710.benchmark;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Core benchmark runner for Nexus 710 performance testing.
 * Executes benchmark tasks and collects timing metrics.
 */
public class BenchmarkRunner {

    public record BenchmarkResult(
        String name,
        int iterations,
        Duration totalDuration,
        Duration avgDuration,
        Duration p50,
        Duration p99,
        Duration min,
        Duration max
    ) {
        @Override
        public String toString() {
            return String.format(
                "[%s] iterations=%d, avg=%dms, p50=%dms, p99=%dms, min=%dms, max=%dms",
                name, iterations,
                avgDuration.toMillis(), p50.toMillis(), p99.toMillis(),
                min.toMillis(), max.toMillis()
            );
        }
    }

    public static BenchmarkResult run(String name, int warmup, int iterations, Callable<?> task) throws Exception {
        for (int i = 0; i < warmup; i++) {
            task.call();
        }

        List<Duration> durations = new ArrayList<>(iterations);
        Instant totalStart = Instant.now();

        for (int i = 0; i < iterations; i++) {
            Instant start = Instant.now();
            task.call();
            durations.add(Duration.between(start, Instant.now()));
        }

        Duration totalDuration = Duration.between(totalStart, Instant.now());
        Collections.sort(durations);

        return new BenchmarkResult(
            name,
            iterations,
            totalDuration,
            totalDuration.dividedBy(iterations),
            durations.get((int) (iterations * 0.50)),
            durations.get((int) (iterations * 0.99)),
            durations.get(0),
            durations.get(iterations - 1)
        );
    }
}
