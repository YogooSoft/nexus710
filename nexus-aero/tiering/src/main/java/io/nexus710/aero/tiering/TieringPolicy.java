/*
 * Licensed under the Apache License, Version 2.0
 * Based on Elasticsearch 7.10.2 (Apache 2.0)
 */

package io.nexus710.aero.tiering;

/**
 * Policy engine for automatic hot/cold data tiering.
 * Determines when index segments should migrate between storage tiers
 * based on age, access frequency, and configured thresholds.
 *
 * Stub for Phase 2 implementation.
 */
public interface TieringPolicy {

    enum StorageTier {
        HOT,
        COLD
    }

    /**
     * Evaluates which tier a given index should reside in.
     */
    StorageTier evaluateTier(String indexName);

    /**
     * Triggers migration of an index to the target tier.
     */
    void migrate(String indexName, StorageTier targetTier);
}
