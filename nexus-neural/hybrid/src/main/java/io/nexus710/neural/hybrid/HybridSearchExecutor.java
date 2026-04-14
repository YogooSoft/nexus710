/*
 * Licensed under the Apache License, Version 2.0
 * Based on Elasticsearch 7.10.2 (Apache 2.0)
 */

package io.nexus710.neural.hybrid;

/**
 * Executes hybrid search combining BM25 text scoring with vector similarity.
 * Uses Reciprocal Rank Fusion (RRF) for score combination.
 *
 * Stub for Phase 1 implementation.
 */
public interface HybridSearchExecutor {

    /**
     * RRF fusion parameters.
     */
    record FusionConfig(float textWeight, float vectorWeight, int rankConstant) {
        public static FusionConfig defaults() {
            return new FusionConfig(0.4f, 0.6f, 60);
        }
    }

    /**
     * A single hybrid search result with fused score.
     */
    record HybridResult(int docId, float fusedScore, float textScore, float vectorScore) {}
}
