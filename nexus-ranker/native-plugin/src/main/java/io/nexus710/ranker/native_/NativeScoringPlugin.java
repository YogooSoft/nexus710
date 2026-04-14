/*
 * Licensed under the Apache License, Version 2.0
 * Based on Elasticsearch 7.10.2 (Apache 2.0)
 */

package io.nexus710.ranker.native_;

/**
 * Interface for native scoring plugins implemented in Rust/C++ via JNI.
 * Provides high-performance scoring operators that bypass JVM interpretation overhead.
 *
 * Stub for Phase 3 implementation.
 */
public interface NativeScoringPlugin {

    /**
     * Loads the native library for this scoring plugin.
     */
    void load(String libraryPath);

    /**
     * Scores a batch of documents using the native scoring operator.
     * Returns scores in the same order as the input document IDs.
     */
    float[] scoreBatch(int[] docIds, float[][] features);

    /**
     * Releases native resources.
     */
    void close();
}
