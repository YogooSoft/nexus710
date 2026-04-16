/*
 * Copyright © 2026 Nexus 710 Contributors
 * Licensed under the Apache License, Version 2.0.
 */

package org.elasticsearch.index.query;

import org.apache.lucene.search.KnnFloatVectorQuery;
import org.apache.lucene.search.Query;
import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.ParsingException;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.index.mapper.MappedFieldType;
import org.elasticsearch.index.mapper.NexusVectorFieldMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Query builder for k-nearest-neighbor search on {@code nexus_vector} fields.
 * Translates to a Lucene {@link KnnFloatVectorQuery}.
 *
 * <p>DSL example:
 * <pre>{@code
 * {
 *   "query": {
 *     "nexus_knn": {
 *       "field": "title_embedding",
 *       "vector": [0.12, -0.34, 0.56, ...],
 *       "k": 10
 *     }
 *   }
 * }
 * }</pre>
 */
public class NexusKnnQueryBuilder extends AbstractQueryBuilder<NexusKnnQueryBuilder> {

    public static final String NAME = "nexus_knn";

    private static final ParseField FIELD_FIELD = new ParseField("field");
    private static final ParseField VECTOR_FIELD = new ParseField("vector");
    private static final ParseField K_FIELD = new ParseField("k");
    private static final ParseField NUM_CANDIDATES_FIELD = new ParseField("num_candidates");

    private final String field;
    private final float[] queryVector;
    private final int k;
    private final int numCandidates;

    public NexusKnnQueryBuilder(String field, float[] queryVector, int k) {
        this(field, queryVector, k, k);
    }

    public NexusKnnQueryBuilder(String field, float[] queryVector, int k, int numCandidates) {
        if (field == null || field.isEmpty()) {
            throw new IllegalArgumentException("[" + NAME + "] requires a field name");
        }
        if (queryVector == null || queryVector.length == 0) {
            throw new IllegalArgumentException("[" + NAME + "] requires a non-empty query vector");
        }
        if (k < 1) {
            throw new IllegalArgumentException("[" + NAME + "] requires k >= 1, got " + k);
        }
        if (numCandidates < k) {
            throw new IllegalArgumentException("[" + NAME + "] num_candidates must be >= k");
        }
        this.field = field;
        this.queryVector = queryVector;
        this.k = k;
        this.numCandidates = numCandidates;
    }

    public NexusKnnQueryBuilder(StreamInput in) throws IOException {
        super(in);
        this.field = in.readString();
        this.queryVector = in.readFloatArray();
        this.k = in.readVInt();
        this.numCandidates = in.readVInt();
    }

    @Override
    protected void doWriteTo(StreamOutput out) throws IOException {
        out.writeString(field);
        out.writeFloatArray(queryVector);
        out.writeVInt(k);
        out.writeVInt(numCandidates);
    }

    @Override
    protected void doXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject(NAME);
        builder.field(FIELD_FIELD.getPreferredName(), field);
        builder.array(VECTOR_FIELD.getPreferredName(), queryVector);
        builder.field(K_FIELD.getPreferredName(), k);
        if (numCandidates != k) {
            builder.field(NUM_CANDIDATES_FIELD.getPreferredName(), numCandidates);
        }
        printBoostAndQueryName(builder);
        builder.endObject();
    }

    public static NexusKnnQueryBuilder fromXContent(XContentParser parser) throws IOException {
        String field = null;
        float[] vector = null;
        int k = 10;
        int numCandidates = -1;
        float boost = AbstractQueryBuilder.DEFAULT_BOOST;
        String queryName = null;

        String currentFieldName = null;
        XContentParser.Token token;
        while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
            if (token == XContentParser.Token.FIELD_NAME) {
                currentFieldName = parser.currentName();
            } else if (FIELD_FIELD.match(currentFieldName, parser.getDeprecationHandler())) {
                field = parser.text();
            } else if (VECTOR_FIELD.match(currentFieldName, parser.getDeprecationHandler())) {
                vector = parseFloatArray(parser);
            } else if (K_FIELD.match(currentFieldName, parser.getDeprecationHandler())) {
                k = parser.intValue();
            } else if (NUM_CANDIDATES_FIELD.match(currentFieldName, parser.getDeprecationHandler())) {
                numCandidates = parser.intValue();
            } else if (AbstractQueryBuilder.BOOST_FIELD.match(currentFieldName, parser.getDeprecationHandler())) {
                boost = parser.floatValue();
            } else if (AbstractQueryBuilder.NAME_FIELD.match(currentFieldName, parser.getDeprecationHandler())) {
                queryName = parser.text();
            } else {
                throw new ParsingException(parser.getTokenLocation(),
                    "[" + NAME + "] unknown field [" + currentFieldName + "]");
            }
        }

        if (field == null) {
            throw new ParsingException(parser.getTokenLocation(), "[" + NAME + "] requires 'field'");
        }
        if (vector == null) {
            throw new ParsingException(parser.getTokenLocation(), "[" + NAME + "] requires 'vector'");
        }
        if (numCandidates < 0) {
            numCandidates = k;
        }

        NexusKnnQueryBuilder builder = new NexusKnnQueryBuilder(field, vector, k, numCandidates);
        builder.boost(boost);
        builder.queryName(queryName);
        return builder;
    }

    private static float[] parseFloatArray(XContentParser parser) throws IOException {
        List<Float> values = new ArrayList<>();
        while (parser.nextToken() != XContentParser.Token.END_ARRAY) {
            values.add(parser.floatValue());
        }
        float[] result = new float[values.size()];
        for (int i = 0; i < values.size(); i++) {
            result[i] = values.get(i);
        }
        return result;
    }

    @Override
    protected Query doToQuery(QueryShardContext context) throws IOException {
        MappedFieldType fieldType = context.fieldMapper(field);
        if (fieldType == null) {
            throw new QueryShardException(context, "field [" + field + "] does not exist");
        }
        if (!(fieldType instanceof NexusVectorFieldMapper.NexusVectorFieldType)) {
            throw new QueryShardException(context, "field [" + field + "] is not a nexus_vector field");
        }

        NexusVectorFieldMapper.NexusVectorFieldType vectorFieldType =
            (NexusVectorFieldMapper.NexusVectorFieldType) fieldType;
        if (queryVector.length != vectorFieldType.dims()) {
            throw new QueryShardException(context,
                "query vector dimension [" + queryVector.length + "] does not match field ["
                    + field + "] dimension [" + vectorFieldType.dims() + "]");
        }

        return new KnnFloatVectorQuery(field, queryVector, numCandidates);
    }

    @Override
    protected boolean doEquals(NexusKnnQueryBuilder other) {
        return Objects.equals(field, other.field)
            && Arrays.equals(queryVector, other.queryVector)
            && k == other.k
            && numCandidates == other.numCandidates;
    }

    @Override
    protected int doHashCode() {
        return Objects.hash(field, Arrays.hashCode(queryVector), k, numCandidates);
    }

    @Override
    public String getWriteableName() {
        return NAME;
    }

    public String field() {
        return field;
    }

    public float[] queryVector() {
        return queryVector;
    }

    public int k() {
        return k;
    }

    public int numCandidates() {
        return numCandidates;
    }
}
