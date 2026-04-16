/*
 * Copyright © 2026 Nexus 710 Contributors
 * Licensed under the Apache License, Version 2.0.
 */

package org.elasticsearch.index.mapper;

import org.apache.lucene.document.KnnFloatVectorField;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.search.FieldExistsQuery;
import org.apache.lucene.search.Query;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentType;

import java.io.IOException;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;

public class NexusVectorFieldMapperTests extends MapperTestCase {

    @Override
    protected void minimalMapping(XContentBuilder b) throws IOException {
        b.field("type", "nexus_vector").field("dims", 3);
    }

    @Override
    protected void registerParameters(ParameterChecker checker) throws IOException {
        checker.registerConflictCheck("dims", fieldMapping(b -> {
            b.field("type", "nexus_vector");
            b.field("dims", 3);
        }), fieldMapping(b -> {
            b.field("type", "nexus_vector");
            b.field("dims", 4);
        }));
        checker.registerConflictCheck("similarity", fieldMapping(b -> {
            b.field("type", "nexus_vector");
            b.field("dims", 3);
            b.field("similarity", "cosine");
        }), fieldMapping(b -> {
            b.field("type", "nexus_vector");
            b.field("dims", 3);
            b.field("similarity", "dot_product");
        }));
    }

    @Override
    protected void writeFieldValue(XContentBuilder builder) throws IOException {
        builder.startArray().value(0.5f).value(0.1f).value(-0.3f).endArray();
    }

    @Override
    protected void assertExistsQuery(MappedFieldType fieldType, Query query, ParseContext.Document fields) {
        assertThat(query, instanceOf(FieldExistsQuery.class));
        FieldExistsQuery existsQuery = (FieldExistsQuery) query;
        assertEquals("field", existsQuery.getField());
    }

    public void testDefaults() throws IOException {
        MapperService mapperService = createMapperService(fieldMapping(b -> {
            b.field("type", "nexus_vector");
            b.field("dims", 4);
        }));
        FieldMapper mapper = (FieldMapper) mapperService.documentMapper().mappers().getMapper("field");
        assertThat(mapper, instanceOf(NexusVectorFieldMapper.class));
        assertThat(mapper.fieldType().typeName(), equalTo("nexus_vector"));

        NexusVectorFieldMapper.NexusVectorFieldType fieldType =
            (NexusVectorFieldMapper.NexusVectorFieldType) mapper.fieldType();
        assertThat(fieldType.dims(), equalTo(4));
    }

    public void testSimilarityOptions() throws IOException {
        for (String similarity : new String[]{"cosine", "dot_product", "l2_norm"}) {
            MapperService mapperService = createMapperService(fieldMapping(b -> {
                b.field("type", "nexus_vector");
                b.field("dims", 3);
                b.field("similarity", similarity);
            }));
            FieldMapper mapper = (FieldMapper) mapperService.documentMapper().mappers().getMapper("field");
            assertThat(mapper, instanceOf(NexusVectorFieldMapper.class));
        }
    }

    public void testInvalidSimilarity() {
        Exception e = expectThrows(MapperParsingException.class, () ->
            createMapperService(fieldMapping(b -> {
                b.field("type", "nexus_vector");
                b.field("dims", 3);
                b.field("similarity", "invalid_sim");
            }))
        );
        assertThat(e.getMessage(), containsString("similarity"));
    }

    public void testDimsValidation() {
        Exception e = expectThrows(MapperParsingException.class, () ->
            createMapperService(fieldMapping(b -> {
                b.field("type", "nexus_vector");
                b.field("dims", 0);
            }))
        );
        assertThat(e.getMessage(), containsString("dims"));

        Exception e2 = expectThrows(MapperParsingException.class, () ->
            createMapperService(fieldMapping(b -> {
                b.field("type", "nexus_vector");
                b.field("dims", 2048);
            }))
        );
        assertThat(e2.getMessage(), containsString("dims"));
    }

    public void testIndexing() throws IOException {
        MapperService mapperService = createMapperService(fieldMapping(b -> {
            b.field("type", "nexus_vector");
            b.field("dims", 3);
        }));

        XContentBuilder doc = XContentFactory.jsonBuilder()
            .startObject()
            .startArray("field")
                .value(1.0f).value(2.0f).value(3.0f)
            .endArray()
            .endObject();

        ParsedDocument parsedDoc = mapperService.documentMapper().parse(
            new SourceToParse("test", "_doc", "1",
                BytesReference.bytes(doc), XContentType.JSON));

        boolean foundVectorField = false;
        for (IndexableField field : parsedDoc.rootDoc().getFields()) {
            if (field.name().equals("field") && field instanceof KnnFloatVectorField) {
                foundVectorField = true;
                KnnFloatVectorField vectorField = (KnnFloatVectorField) field;
                float[] values = vectorField.vectorValue();
                assertThat(values.length, equalTo(3));
                assertEquals(1.0f, values[0], 0.001f);
                assertEquals(2.0f, values[1], 0.001f);
                assertEquals(3.0f, values[2], 0.001f);
            }
        }
        assertTrue("Expected a KnnFloatVectorField in parsed document", foundVectorField);
    }

    public void testDimensionMismatch() throws IOException {
        MapperService mapperService = createMapperService(fieldMapping(b -> {
            b.field("type", "nexus_vector");
            b.field("dims", 3);
        }));

        XContentBuilder doc = XContentFactory.jsonBuilder()
            .startObject()
            .startArray("field")
                .value(1.0f).value(2.0f)
            .endArray()
            .endObject();

        Exception e = expectThrows(MapperParsingException.class, () ->
            mapperService.documentMapper().parse(
                new SourceToParse("test", "_doc", "1",
                    BytesReference.bytes(doc), XContentType.JSON)));
        assertThat(e.getCause().getMessage(), containsString("dimension mismatch"));
    }

    public void testMeta() throws IOException {
        MapperService mapperService = createMapperService(fieldMapping(b -> {
            b.field("type", "nexus_vector");
            b.field("dims", 3);
            b.startObject("meta").field("unit", "normalized").endObject();
        }));
        FieldMapper mapper = (FieldMapper) mapperService.documentMapper().mappers().getMapper("field");
        assertThat(mapper.fieldType().meta().get("unit"), equalTo("normalized"));
    }

    public void testSerialization() throws IOException {
        MapperService mapperService = createMapperService(fieldMapping(b -> {
            b.field("type", "nexus_vector");
            b.field("dims", 128);
            b.field("similarity", "dot_product");
            b.field("m", 32);
            b.field("ef_construction", 400);
        }));

        String serialized = Strings.toString(
            mapperService.documentMapper().mapping());
        assertThat(serialized, containsString("nexus_vector"));
        assertThat(serialized, containsString("128"));
        assertThat(serialized, containsString("dot_product"));
    }
}
