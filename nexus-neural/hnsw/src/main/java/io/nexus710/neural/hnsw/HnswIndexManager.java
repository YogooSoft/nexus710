/*
 * Licensed under the Apache License, Version 2.0
 * Based on Elasticsearch 7.10.2 (Apache 2.0)
 */

package io.nexus710.neural.hnsw;

/**
 * Manages HNSW (Hierarchical Navigable Small World) vector indices
 * integrated into Lucene segment lifecycle.
 *
 * This is a stub for Phase 1 implementation. The actual implementation
 * will integrate Lucene 9.x KnnVectorField with segment merge logic.
 */
public interface HnswIndexManager {

    /**
     * Configuration for HNSW index construction.
     */
    record HnswConfig(int m, int efConstruction, int efSearch, String similarity) {
        public static HnswConfig defaults() {
            return new HnswConfig(16, 200, 100, "cosine");
        }
    }

    /**
     * Indexes a vector for the given document.
     */
    void indexVector(String field, int docId, float[] vector);

    /**
     * Searches for the k nearest neighbors to the given query vector.
     */
    int[] searchKnn(String field, float[] queryVector, int k);
}
