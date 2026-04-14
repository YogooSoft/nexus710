/*
 * Licensed under the Apache License, Version 2.0
 * Based on Elasticsearch 7.10.2 (Apache 2.0)
 */

package io.nexus710.ranker.featurejoin;

import java.util.Map;

/**
 * Executes real-time feature joins during the search fetch phase.
 * Associates external features (user profiles, inventory, pricing)
 * with candidate documents for precision re-ranking.
 *
 * Stub for Phase 3 implementation.
 */
public interface FeatureJoinExecutor {

    /**
     * Joins external features for the given document IDs.
     * Returns a map of docId -> feature vector.
     */
    Map<Integer, float[]> joinFeatures(int[] docIds, String featureSource);
}
