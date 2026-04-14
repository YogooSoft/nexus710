/*
 * Licensed under the Apache License, Version 2.0
 * Based on Elasticsearch 7.10.2 (Apache 2.0)
 */

package io.nexus710.benchmark;

/**
 * Defines benchmark suites for different Nexus 710 components.
 * Each suite targets a specific performance aspect aligned with the
 * development plan milestones.
 */
public enum BenchmarkSuite {

    /**
     * Text search baseline using ES Rally geonames track.
     * Target: match or exceed ES 7.10 baseline throughput.
     */
    TEXT_SEARCH_BASELINE("Text Search Baseline (geonames)"),

    /**
     * Vector search using ANN-Benchmarks SIFT1M dataset.
     * Target: Recall@10 >= 95%, P99 < 50ms (1M vectors).
     */
    VECTOR_SEARCH_SIFT1M("Vector Search (SIFT1M, 128d)"),

    /**
     * Vector search using ANN-Benchmarks GloVe-200 dataset.
     * Target: High-dimensional vector performance validation.
     */
    VECTOR_SEARCH_GLOVE200("Vector Search (GloVe-200, 200d)"),

    /**
     * Hybrid search combining text + vector queries.
     * Target: RRF fusion overhead < 5% vs individual queries.
     */
    HYBRID_SEARCH("Hybrid Search (BM25 + Vector)"),

    /**
     * S3 storage read/write with block cache.
     * Target: Cache hit < 5ms, cache miss < 200ms.
     */
    STORAGE_S3_CACHE("S3 Storage + Block Cache"),

    /**
     * Full-stack stress test.
     * Target: P99 < 100ms, 72h stability zero crash.
     */
    FULL_STACK_STRESS("Full Stack Stress Test"),

    /**
     * Aggregation performance with SIMD optimization.
     * Target: 2-4x improvement over baseline.
     */
    AGGREGATION_SIMD("Aggregation (SIMD Optimized)");

    private final String description;

    BenchmarkSuite(String description) {
        this.description = description;
    }

    public String description() {
        return description;
    }
}
