/*
 * Licensed under the Apache License, Version 2.0
 * Based on Elasticsearch 7.10.2 (Apache 2.0)
 */

package io.nexus710.benchmark;

import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.query.QueryBuilders;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Locale;
import java.util.Random;

/**
 * Phase 0 baseline performance test for text search.
 * Indexes synthetic "geonames-like" documents and benchmarks
 * match / term / bool queries against a running ES node via the transport client.
 *
 * <p>Usage: instantiate with an ES {@link Client}, call {@link #run()}.
 */
public class TextSearchBaseline {

    private static final int DOC_COUNT = 50_000;
    private static final int WARMUP = 5;
    private static final int ITERATIONS = 100;
    private static final String INDEX = "bench_geonames";
    private static final PrintStream OUT = System.out;

    private static final String[] COUNTRIES = {
        "CN", "US", "GB", "DE", "JP", "FR", "IN", "BR", "RU", "KR"
    };
    private static final String[] CITIES = {
        "Beijing", "Shanghai", "New York", "London", "Berlin", "Tokyo",
        "Paris", "Mumbai", "Sao Paulo", "Moscow", "Seoul", "Sydney",
        "Toronto", "Singapore", "Dubai", "Amsterdam", "Stockholm"
    };
    private static final String[] FEATURES = {
        "mountain", "river", "lake", "city", "village", "island",
        "valley", "plateau", "forest", "desert", "bay", "cape"
    };

    private final Client client;
    private final Random random = new Random(42);

    public TextSearchBaseline(Client client) {
        this.client = client;
    }

    public void run() throws Exception {
        OUT.println("=== Nexus 710 Text Search Baseline ===\n");

        createIndex();
        indexDocuments();
        client.admin().indices().prepareRefresh(INDEX).get();

        BenchmarkRunner.BenchmarkResult matchResult = BenchmarkRunner.run(
            BenchmarkSuite.TEXT_SEARCH_BASELINE.description() + " — match",
            WARMUP, ITERATIONS,
            () -> {
                SearchResponse resp = client.prepareSearch(INDEX)
                    .setQuery(QueryBuilders.matchQuery("name", randomCity()))
                    .setSize(10)
                    .get();
                return resp.getHits().getTotalHits().value;
            }
        );

        BenchmarkRunner.BenchmarkResult termResult = BenchmarkRunner.run(
            BenchmarkSuite.TEXT_SEARCH_BASELINE.description() + " — term",
            WARMUP, ITERATIONS,
            () -> {
                SearchResponse resp = client.prepareSearch(INDEX)
                    .setQuery(QueryBuilders.termQuery("country_code", randomCountry().toLowerCase(Locale.ROOT)))
                    .setSize(10)
                    .get();
                return resp.getHits().getTotalHits().value;
            }
        );

        BenchmarkRunner.BenchmarkResult boolResult = BenchmarkRunner.run(
            BenchmarkSuite.TEXT_SEARCH_BASELINE.description() + " — bool",
            WARMUP, ITERATIONS,
            () -> {
                SearchResponse resp = client.prepareSearch(INDEX)
                    .setQuery(QueryBuilders.boolQuery()
                        .must(QueryBuilders.matchQuery("name", randomCity()))
                        .filter(QueryBuilders.termQuery("country_code", randomCountry().toLowerCase(Locale.ROOT)))
                    )
                    .setSize(10)
                    .get();
                return resp.getHits().getTotalHits().value;
            }
        );

        OUT.println("\n=== Results ===");
        OUT.println(matchResult);
        OUT.println(termResult);
        OUT.println(boolResult);

        client.admin().indices().prepareDelete(INDEX).get();
        OUT.println("\nBaseline complete.");
    }

    private void createIndex() throws IOException {
        if (client.admin().indices().prepareExists(INDEX).get().isExists()) {
            client.admin().indices().prepareDelete(INDEX).get();
        }

        XContentBuilder mapping = XContentFactory.jsonBuilder()
            .startObject()
                .startObject("properties")
                    .startObject("name")
                        .field("type", "text")
                    .endObject()
                    .startObject("country_code")
                        .field("type", "keyword")
                    .endObject()
                    .startObject("feature_class")
                        .field("type", "keyword")
                    .endObject()
                    .startObject("population")
                        .field("type", "long")
                    .endObject()
                .endObject()
            .endObject();

        client.admin().indices().prepareCreate(INDEX)
            .setSettings(Settings.builder()
                .put("number_of_shards", 1)
                .put("number_of_replicas", 0))
            .addMapping("_doc", mapping)
            .get();
    }

    private void indexDocuments() throws IOException {
        int batchSize = 5000;
        for (int batch = 0; batch < DOC_COUNT / batchSize; batch++) {
            BulkRequestBuilder bulk = client.prepareBulk();
            for (int i = 0; i < batchSize; i++) {
                int docId = batch * batchSize + i;
                XContentBuilder doc = XContentFactory.jsonBuilder()
                    .startObject()
                        .field("name", generateName(docId))
                        .field("country_code", randomCountry().toLowerCase(Locale.ROOT))
                        .field("feature_class", FEATURES[random.nextInt(FEATURES.length)])
                        .field("population", random.nextInt(10_000_000))
                    .endObject();
                bulk.add(client.prepareIndex().setIndex(INDEX).setSource(doc));
            }
            bulk.get();
        }
    }

    private String generateName(int seed) {
        return CITIES[seed % CITIES.length] + " " + FEATURES[seed % FEATURES.length] + " " + seed;
    }

    private String randomCity() {
        return CITIES[random.nextInt(CITIES.length)];
    }

    private String randomCountry() {
        return COUNTRIES[random.nextInt(COUNTRIES.length)];
    }
}
