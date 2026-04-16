/*
 * Copyright © 2026 Nexus 710 Contributors
 * Licensed under the Apache License, Version 2.0.
 */

package org.elasticsearch.search;

import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.query.NexusKnnQueryBuilder;
import org.elasticsearch.test.ESIntegTestCase;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.elasticsearch.test.hamcrest.ElasticsearchAssertions.assertHitCount;
import static org.hamcrest.Matchers.greaterThan;

/**
 * Integration test for the nexus_vector field type and nexus_knn query.
 * Validates the full end-to-end flow: create index with vector mapping,
 * index documents with vector data, and perform KNN search.
 */
@ESIntegTestCase.ClusterScope(scope = ESIntegTestCase.Scope.TEST, numDataNodes = 1)
public class NexusVectorSearchIT extends ESIntegTestCase {

    private static final int DIMS = 4;
    private static final String INDEX = "vector_test";
    private static final String FIELD = "embedding";

    public void testIndexAndSearchVectors() throws Exception {
        createVectorIndex(DIMS, "cosine");
        indexVectorDocuments();
        refresh(INDEX);

        float[] queryVector = new float[]{1.0f, 0.0f, 0.0f, 0.0f};
        SearchResponse response = client().prepareSearch(INDEX)
            .setQuery(new NexusKnnQueryBuilder(FIELD, queryVector, 3))
            .setSize(3)
            .get();

        assertHitCount(response, 3);
        assertThat(response.getHits().getMaxScore(), greaterThan(0.0f));
    }

    public void testVectorSearchWithDotProduct() throws Exception {
        createVectorIndex(DIMS, "dot_product");
        indexNormalizedDocuments();
        refresh(INDEX);

        float[] queryVector = normalizeVector(new float[]{1.0f, 0.0f, 0.0f, 0.0f});
        SearchResponse response = client().prepareSearch(INDEX)
            .setQuery(new NexusKnnQueryBuilder(FIELD, queryVector, 5))
            .setSize(5)
            .get();

        assertHitCount(response, 5);
    }

    public void testVectorSearchWithL2Norm() throws Exception {
        createVectorIndex(DIMS, "l2_norm");
        indexVectorDocuments();
        refresh(INDEX);

        float[] queryVector = new float[]{0.0f, 0.0f, 0.0f, 0.0f};
        SearchResponse response = client().prepareSearch(INDEX)
            .setQuery(new NexusKnnQueryBuilder(FIELD, queryVector, 3))
            .setSize(3)
            .get();

        assertHitCount(response, 3);
    }

    public void testMixedFieldsWithVectors() throws Exception {
        XContentBuilder mapping = XContentFactory.jsonBuilder()
            .startObject()
                .startObject("properties")
                    .startObject("title")
                        .field("type", "text")
                    .endObject()
                    .startObject(FIELD)
                        .field("type", "nexus_vector")
                        .field("dims", DIMS)
                        .field("similarity", "cosine")
                    .endObject()
                .endObject()
            .endObject();

        client().admin().indices().prepareCreate(INDEX)
            .setSettings(Settings.builder()
                .put("number_of_shards", 1)
                .put("number_of_replicas", 0))
            .addMapping("_doc", mapping)
            .get();

        List<IndexRequestBuilder> requests = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            float[] vec = new float[DIMS];
            for (int d = 0; d < DIMS; d++) {
                vec[d] = (float) Math.sin(i + d);
            }
            XContentBuilder doc = XContentFactory.jsonBuilder()
                .startObject()
                    .field("title", "document " + i)
                    .startArray(FIELD);
            for (float v : vec) {
                doc.value(v);
            }
            doc.endArray().endObject();
            requests.add(client().prepareIndex(INDEX, "_doc").setSource(doc));
        }
        indexRandom(true, requests);

        float[] queryVector = new float[]{0.5f, 0.5f, 0.5f, 0.5f};
        SearchResponse response = client().prepareSearch(INDEX)
            .setQuery(new NexusKnnQueryBuilder(FIELD, queryVector, 5))
            .setSize(5)
            .get();

        assertHitCount(response, 5);
    }

    public void testDimensionMismatchAtQueryTime() throws Exception {
        createVectorIndex(DIMS, "cosine");
        indexVectorDocuments();
        refresh(INDEX);

        float[] wrongDimVector = new float[]{1.0f, 0.0f};
        try {
            client().prepareSearch(INDEX)
                .setQuery(new NexusKnnQueryBuilder(FIELD, wrongDimVector, 3))
                .setSize(3)
                .get();
            fail("Expected query to fail due to dimension mismatch");
        } catch (Exception e) {
            String fullMessage = getFullExceptionMessage(e);
            assertThat(fullMessage,
                org.hamcrest.Matchers.containsString("dimension"));
        }
    }

    private static String getFullExceptionMessage(Throwable t) {
        StringBuilder sb = new StringBuilder();
        while (t != null) {
            if (t.getMessage() != null) {
                sb.append(t.getMessage()).append(" ");
            }
            t = t.getCause();
        }
        return sb.toString();
    }

    private void createVectorIndex(int dims, String similarity) throws IOException {
        XContentBuilder mapping = XContentFactory.jsonBuilder()
            .startObject()
                .startObject("properties")
                    .startObject(FIELD)
                        .field("type", "nexus_vector")
                        .field("dims", dims)
                        .field("similarity", similarity)
                    .endObject()
                .endObject()
            .endObject();

        client().admin().indices().prepareCreate(INDEX)
            .setSettings(Settings.builder()
                .put("number_of_shards", 1)
                .put("number_of_replicas", 0))
            .addMapping("_doc", mapping)
            .get();
    }

    private void indexVectorDocuments() throws Exception {
        List<IndexRequestBuilder> requests = new ArrayList<>();
        float[][] vectors = {
            {1.0f, 0.0f, 0.0f, 0.0f},
            {0.0f, 1.0f, 0.0f, 0.0f},
            {0.0f, 0.0f, 1.0f, 0.0f},
            {0.0f, 0.0f, 0.0f, 1.0f},
            {0.5f, 0.5f, 0.0f, 0.0f},
            {0.0f, 0.5f, 0.5f, 0.0f},
            {0.5f, 0.0f, 0.5f, 0.0f},
            {0.3f, 0.3f, 0.3f, 0.3f},
            {0.7f, 0.1f, 0.1f, 0.1f},
            {0.1f, 0.7f, 0.1f, 0.1f},
        };
        for (int i = 0; i < vectors.length; i++) {
            XContentBuilder doc = XContentFactory.jsonBuilder()
                .startObject()
                .startArray(FIELD);
            for (float v : vectors[i]) {
                doc.value(v);
            }
            doc.endArray().endObject();
            requests.add(client().prepareIndex(INDEX, "_doc", String.valueOf(i)).setSource(doc));
        }
        indexRandom(true, requests);
    }

    private void indexNormalizedDocuments() throws Exception {
        List<IndexRequestBuilder> requests = new ArrayList<>();
        float[][] vectors = {
            normalizeVector(new float[]{1.0f, 0.0f, 0.0f, 0.0f}),
            normalizeVector(new float[]{0.0f, 1.0f, 0.0f, 0.0f}),
            normalizeVector(new float[]{0.0f, 0.0f, 1.0f, 0.0f}),
            normalizeVector(new float[]{0.0f, 0.0f, 0.0f, 1.0f}),
            normalizeVector(new float[]{0.5f, 0.5f, 0.0f, 0.0f}),
            normalizeVector(new float[]{0.0f, 0.5f, 0.5f, 0.0f}),
            normalizeVector(new float[]{0.5f, 0.0f, 0.5f, 0.0f}),
            normalizeVector(new float[]{0.3f, 0.3f, 0.3f, 0.3f}),
            normalizeVector(new float[]{0.7f, 0.1f, 0.1f, 0.1f}),
            normalizeVector(new float[]{0.1f, 0.7f, 0.1f, 0.1f}),
        };
        for (int i = 0; i < vectors.length; i++) {
            XContentBuilder doc = XContentFactory.jsonBuilder()
                .startObject()
                .startArray(FIELD);
            for (float v : vectors[i]) {
                doc.value(v);
            }
            doc.endArray().endObject();
            requests.add(client().prepareIndex(INDEX, "_doc", String.valueOf(i)).setSource(doc));
        }
        indexRandom(true, requests);
    }

    private static float[] normalizeVector(float[] vec) {
        float norm = 0;
        for (float v : vec) {
            norm += v * v;
        }
        norm = (float) Math.sqrt(norm);
        if (norm == 0) return vec;
        float[] result = new float[vec.length];
        for (int i = 0; i < vec.length; i++) {
            result[i] = vec[i] / norm;
        }
        return result;
    }
}
