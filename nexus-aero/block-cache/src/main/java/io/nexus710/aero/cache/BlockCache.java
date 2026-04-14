/*
 * Licensed under the Apache License, Version 2.0
 * Based on Elasticsearch 7.10.2 (Apache 2.0)
 */

package io.nexus710.aero.cache;

import java.io.IOException;

/**
 * Multi-level block cache with weighted LRU eviction.
 * <p>
 * L1: Heap memory (256MB) — hottest metadata blocks
 * L2: mmap SSD (configurable, default 100GB) — hot data blocks
 * L3: S3/object storage — full dataset
 * <p>
 * Eviction weight = accessCount * recencyScore * blockSizePenalty
 *
 * Stub for Phase 2 implementation.
 */
public interface BlockCache {

    record CacheKey(String segmentId, long blockOffset) {}

    record CacheStats(long hits, long misses, long evictions, double hitRate) {}

    byte[] get(CacheKey key) throws IOException;

    void put(CacheKey key, byte[] data);

    void invalidate(CacheKey key);

    CacheStats stats();
}
