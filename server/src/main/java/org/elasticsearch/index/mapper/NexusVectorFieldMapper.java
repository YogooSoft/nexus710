/*
 * Copyright © 2026 Nexus 710 Contributors
 * Licensed under the Apache License, Version 2.0.
 */

package org.elasticsearch.index.mapper;

import org.apache.lucene.document.KnnFloatVectorField;
import org.apache.lucene.index.VectorSimilarityFunction;
import org.apache.lucene.search.Query;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.index.fielddata.IndexFieldData;
import org.elasticsearch.index.query.QueryShardContext;
import org.elasticsearch.search.lookup.SearchLookup;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Field mapper for dense vector fields using Lucene 9.x HNSW vector indexing.
 * Supports cosine, dot_product, and l2_norm similarity functions.
 *
 * <p>Mapping example:
 * <pre>{@code
 * {
 *   "my_vector": {
 *     "type": "nexus_vector",
 *     "dims": 128,
 *     "similarity": "cosine",
 *     "index_options": {
 *       "type": "hnsw",
 *       "m": 16,
 *       "ef_construction": 200
 *     }
 *   }
 * }
 * }</pre>
 */
public final class NexusVectorFieldMapper extends ParametrizedFieldMapper {

    public static final String CONTENT_TYPE = "nexus_vector";
    public static final int MAX_DIMS = 1024;
    public static final int DEFAULT_DIMS = 128;
    public static final String DEFAULT_SIMILARITY = "cosine";
    public static final int DEFAULT_M = 16;
    public static final int DEFAULT_EF_CONSTRUCTION = 200;

    private static NexusVectorFieldMapper toType(FieldMapper in) {
        return (NexusVectorFieldMapper) in;
    }

    public static class Builder extends ParametrizedFieldMapper.Builder {

        private final Parameter<Integer> dims =
            new Parameter<>("dims", false, () -> DEFAULT_DIMS,
                (n, c, o) -> {
                    int d = XContentMapValueUtils.nodeIntegerValue(o);
                    if (d < 1 || d > MAX_DIMS) {
                        throw new MapperParsingException("dims for [" + n + "] must be between 1 and " + MAX_DIMS + ", got " + d);
                    }
                    return d;
                },
                m -> toType(m).dims).alwaysSerialize();

        private final Parameter<String> similarity =
            Parameter.restrictedStringParam("similarity", false,
                m -> toType(m).similarity,
                "cosine", "dot_product", "l2_norm")
                .alwaysSerialize();

        private final Parameter<Integer> m =
            new Parameter<>("m", false, () -> DEFAULT_M,
                (n, c, o) -> XContentMapValueUtils.nodeIntegerValue(o),
                mapper -> toType(mapper).hnswM);

        private final Parameter<Integer> efConstruction =
            new Parameter<>("ef_construction", false, () -> DEFAULT_EF_CONSTRUCTION,
                (n, c, o) -> XContentMapValueUtils.nodeIntegerValue(o),
                mapper -> toType(mapper).efConstruction);

        private final Parameter<Map<String, String>> meta = Parameter.metaParam();

        public Builder(String name) {
            super(name);
        }

        @Override
        protected List<Parameter<?>> getParameters() {
            return Arrays.asList(dims, similarity, m, efConstruction, meta);
        }

        @Override
        public NexusVectorFieldMapper build(BuilderContext context) {
            VectorSimilarityFunction simFunc = parseSimilarity(similarity.getValue());
            NexusVectorFieldType fieldType = new NexusVectorFieldType(
                buildFullName(context), dims.getValue(), simFunc, meta.getValue());
            return new NexusVectorFieldMapper(name, fieldType,
                multiFieldsBuilder.build(this, context), copyTo.build(), this);
        }
    }

    public static final TypeParser PARSER = new TypeParser((n, c) -> new Builder(n));

    public static final class NexusVectorFieldType extends MappedFieldType {

        private final int dims;
        private final VectorSimilarityFunction similarityFunction;

        public NexusVectorFieldType(String name, int dims, VectorSimilarityFunction similarityFunction,
                                    Map<String, String> meta) {
            super(name, true, false, false, TextSearchInfo.NONE, meta);
            this.dims = dims;
            this.similarityFunction = similarityFunction;
        }

        @Override
        public String typeName() {
            return CONTENT_TYPE;
        }

        public int dims() {
            return dims;
        }

        public VectorSimilarityFunction similarityFunction() {
            return similarityFunction;
        }

        @Override
        public Query termQuery(Object value, QueryShardContext context) {
            throw new IllegalArgumentException("Field [" + name() + "] of type [" + typeName()
                + "] does not support term queries; use nexus_knn query instead.");
        }

        @Override
        public Query existsQuery(QueryShardContext context) {
            return new org.apache.lucene.search.FieldExistsQuery(name());
        }

        @Override
        public IndexFieldData.Builder fielddataBuilder(String fullyQualifiedIndexName, Supplier<SearchLookup> searchLookup) {
            throw new IllegalArgumentException("Field [" + name() + "] of type [" + typeName()
                + "] does not support fielddata access.");
        }

        @Override
        public ValueFetcher valueFetcher(MapperService mapperService, SearchLookup searchLookup, String format) {
            if (format != null) {
                throw new IllegalArgumentException("Field [" + name() + "] of type [" + typeName()
                    + "] doesn't support formats.");
            }
            return new ArraySourceValueFetcher(name(), mapperService) {
                @Override
                protected Object parseSourceValue(Object value) {
                    return value;
                }
            };
        }
    }

    private final int dims;
    private final String similarity;
    private final int hnswM;
    private final int efConstruction;
    private final VectorSimilarityFunction similarityFunction;

    private NexusVectorFieldMapper(String simpleName, NexusVectorFieldType mappedFieldType,
                                   MultiFields multiFields, CopyTo copyTo, Builder builder) {
        super(simpleName, mappedFieldType, multiFields, copyTo);
        this.dims = builder.dims.getValue();
        this.similarity = builder.similarity.getValue();
        this.hnswM = builder.m.getValue();
        this.efConstruction = builder.efConstruction.getValue();
        this.similarityFunction = parseSimilarity(this.similarity);
    }

    @Override
    public NexusVectorFieldType fieldType() {
        return (NexusVectorFieldType) super.fieldType();
    }

    @Override
    protected void parseCreateField(ParseContext context) throws IOException {
        XContentParser parser = context.parser();
        float[] vector = parseVector(parser);

        if (vector.length != dims) {
            throw new MapperParsingException("Vector dimension mismatch for field [" + name()
                + "]: expected " + dims + " but got " + vector.length);
        }

        KnnFloatVectorField field = new KnnFloatVectorField(name(), vector, similarityFunction);
        context.doc().add(field);
    }

    private float[] parseVector(XContentParser parser) throws IOException {
        List<Float> values = new ArrayList<>();
        if (parser.currentToken() == XContentParser.Token.START_ARRAY) {
            XContentParser.Token token;
            while ((token = parser.nextToken()) != XContentParser.Token.END_ARRAY) {
                if (token == XContentParser.Token.VALUE_NUMBER) {
                    values.add(parser.floatValue());
                } else {
                    throw new MapperParsingException("Expected float value in vector array for field ["
                        + name() + "], got " + token);
                }
            }
        } else {
            throw new MapperParsingException("Expected array for vector field [" + name()
                + "], got " + parser.currentToken());
        }
        float[] result = new float[values.size()];
        for (int i = 0; i < values.size(); i++) {
            result[i] = values.get(i);
        }
        return result;
    }

    @Override
    public boolean parsesArrayValue() {
        return true;
    }

    @Override
    protected String contentType() {
        return CONTENT_TYPE;
    }

    @Override
    public ParametrizedFieldMapper.Builder getMergeBuilder() {
        return new Builder(simpleName()).init(this);
    }

    static VectorSimilarityFunction parseSimilarity(String similarity) {
        switch (similarity) {
            case "cosine":
                return VectorSimilarityFunction.COSINE;
            case "dot_product":
                return VectorSimilarityFunction.DOT_PRODUCT;
            case "l2_norm":
                return VectorSimilarityFunction.EUCLIDEAN;
            default:
                throw new MapperParsingException("Unknown similarity [" + similarity
                    + "]; supported values: cosine, dot_product, l2_norm");
        }
    }

    private static class XContentMapValueUtils {
        static int nodeIntegerValue(Object o) {
            if (o instanceof Number) {
                return ((Number) o).intValue();
            }
            return Integer.parseInt(o.toString());
        }
    }
}
