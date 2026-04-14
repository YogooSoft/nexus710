/*
 * Licensed under the Apache License, Version 2.0
 * Based on Elasticsearch 7.10.2 (Apache 2.0)
 */

package io.nexus710.ranker.circuitbreaker;

/**
 * Dynamic circuit breaker that tracks memory usage at query granularity.
 * Unlike the original ES circuit breaker which rejects entire requests,
 * this supports partial degradation — returning partial results instead
 * of failing completely.
 *
 * Stub for Phase 3 implementation.
 */
public interface DynamicCircuitBreaker {

    enum BreakerState {
        CLOSED,
        HALF_OPEN,
        OPEN
    }

    /**
     * Attempts to reserve memory for a query operation.
     * Returns true if the reservation succeeded.
     */
    boolean tryReserve(String queryId, long bytes);

    /**
     * Releases reserved memory for a completed query.
     */
    void release(String queryId);

    /**
     * Returns the current state of the circuit breaker.
     */
    BreakerState state();
}
