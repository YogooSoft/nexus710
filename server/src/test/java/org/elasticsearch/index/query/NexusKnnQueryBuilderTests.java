/*
 * Copyright © 2026 Nexus 710 Contributors
 * Licensed under the Apache License, Version 2.0.
 */

package org.elasticsearch.index.query;

import org.elasticsearch.common.Strings;
import org.elasticsearch.common.io.stream.BytesStreamOutput;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.test.ESTestCase;

import java.io.IOException;
import java.util.Arrays;

import static org.hamcrest.Matchers.equalTo;

public class NexusKnnQueryBuilderTests extends ESTestCase {

    private static final float[] TEST_VECTOR = new float[]{0.1f, 0.2f, 0.3f};

    public void testConstructor() {
        NexusKnnQueryBuilder builder = new NexusKnnQueryBuilder("my_field", TEST_VECTOR, 10);
        assertThat(builder.field(), equalTo("my_field"));
        assertTrue(Arrays.equals(builder.queryVector(), TEST_VECTOR));
        assertThat(builder.k(), equalTo(10));
        assertThat(builder.numCandidates(), equalTo(10));
    }

    public void testConstructorWithNumCandidates() {
        NexusKnnQueryBuilder builder = new NexusKnnQueryBuilder("my_field", TEST_VECTOR, 10, 50);
        assertThat(builder.numCandidates(), equalTo(50));
    }

    public void testConstructorValidation() {
        expectThrows(IllegalArgumentException.class,
            () -> new NexusKnnQueryBuilder(null, TEST_VECTOR, 10));
        expectThrows(IllegalArgumentException.class,
            () -> new NexusKnnQueryBuilder("", TEST_VECTOR, 10));
        expectThrows(IllegalArgumentException.class,
            () -> new NexusKnnQueryBuilder("field", null, 10));
        expectThrows(IllegalArgumentException.class,
            () -> new NexusKnnQueryBuilder("field", new float[0], 10));
        expectThrows(IllegalArgumentException.class,
            () -> new NexusKnnQueryBuilder("field", TEST_VECTOR, 0));
        expectThrows(IllegalArgumentException.class,
            () -> new NexusKnnQueryBuilder("field", TEST_VECTOR, 10, 5));
    }

    public void testSerialization() throws IOException {
        NexusKnnQueryBuilder original = new NexusKnnQueryBuilder("my_field", TEST_VECTOR, 10, 50);
        original.boost(1.5f);
        original.queryName("test_query");

        BytesStreamOutput out = new BytesStreamOutput();
        original.writeTo(out);

        StreamInput in = out.bytes().streamInput();
        NexusKnnQueryBuilder deserialized = new NexusKnnQueryBuilder(in);

        assertThat(deserialized.field(), equalTo(original.field()));
        assertTrue(Arrays.equals(deserialized.queryVector(), original.queryVector()));
        assertThat(deserialized.k(), equalTo(original.k()));
        assertThat(deserialized.numCandidates(), equalTo(original.numCandidates()));
        assertThat(deserialized.boost(), equalTo(original.boost()));
        assertThat(deserialized.queryName(), equalTo(original.queryName()));
    }

    public void testToXContent() throws IOException {
        NexusKnnQueryBuilder builder = new NexusKnnQueryBuilder("my_field", TEST_VECTOR, 5, 20);

        XContentBuilder xContentBuilder = XContentFactory.jsonBuilder();
        xContentBuilder.startObject();
        builder.doXContent(xContentBuilder, null);
        xContentBuilder.endObject();
        String json = Strings.toString(xContentBuilder);

        assertTrue(json.contains("\"field\":\"my_field\""));
        assertTrue(json.contains("\"k\":5"));
        assertTrue(json.contains("\"num_candidates\":20"));
        assertTrue(json.contains("\"vector\""));
    }

    public void testFromXContent() throws IOException {
        String json = "{\"field\":\"my_field\",\"vector\":[0.1,0.2,0.3],\"k\":5,\"num_candidates\":20}";
        XContentParser parser = createParser(XContentType.JSON.xContent(), json);
        parser.nextToken();

        NexusKnnQueryBuilder parsed = NexusKnnQueryBuilder.fromXContent(parser);
        assertThat(parsed.field(), equalTo("my_field"));
        assertThat(parsed.k(), equalTo(5));
        assertThat(parsed.numCandidates(), equalTo(20));
        assertThat(parsed.queryVector().length, equalTo(3));
    }

    public void testFromXContentMinimal() throws IOException {
        String json = "{\"field\":\"my_field\",\"vector\":[0.1,0.2,0.3],\"k\":10}";
        XContentParser parser = createParser(XContentType.JSON.xContent(), json);
        parser.nextToken();

        NexusKnnQueryBuilder parsed = NexusKnnQueryBuilder.fromXContent(parser);
        assertThat(parsed.field(), equalTo("my_field"));
        assertThat(parsed.k(), equalTo(10));
        assertThat(parsed.numCandidates(), equalTo(10));
    }

    public void testFromXContentMissingField() throws IOException {
        String json = "{\"vector\":[0.1,0.2,0.3],\"k\":10}";
        XContentParser parser = createParser(XContentType.JSON.xContent(), json);
        parser.nextToken();

        expectThrows(Exception.class, () -> NexusKnnQueryBuilder.fromXContent(parser));
    }

    public void testFromXContentMissingVector() throws IOException {
        String json = "{\"field\":\"my_field\",\"k\":10}";
        XContentParser parser = createParser(XContentType.JSON.xContent(), json);
        parser.nextToken();

        expectThrows(Exception.class, () -> NexusKnnQueryBuilder.fromXContent(parser));
    }

    public void testEqualsAndHashCode() {
        NexusKnnQueryBuilder a = new NexusKnnQueryBuilder("field", TEST_VECTOR, 10);
        NexusKnnQueryBuilder b = new NexusKnnQueryBuilder("field", TEST_VECTOR, 10);
        assertTrue(a.equals(b));
        assertThat(a.hashCode(), equalTo(b.hashCode()));

        NexusKnnQueryBuilder c = new NexusKnnQueryBuilder("other_field", TEST_VECTOR, 10);
        assertFalse(a.equals(c));
    }

    public void testGetWriteableName() {
        NexusKnnQueryBuilder builder = new NexusKnnQueryBuilder("field", TEST_VECTOR, 10);
        assertThat(builder.getWriteableName(), equalTo("nexus_knn"));
    }
}
