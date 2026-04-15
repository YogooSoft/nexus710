/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.lucene.queries;

// TODO: Lucene 9.x migration - spans removed
// This class previously extended SpanQuery. Now delegates to MatchNoDocsQuery.

import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchNoDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.QueryVisitor;
import org.apache.lucene.search.ScoreMode;
import org.apache.lucene.search.Weight;

import java.io.IOException;

/**
 * A Query that matches no documents (stub replacement for the removed SpanQuery-based version).
 */
public class SpanMatchNoDocsQuery extends Query {
    private final String field;
    private final String reason;

    public SpanMatchNoDocsQuery(String field, String reason) {
        this.field = field;
        this.reason = reason;
    }

    public String getField() {
        return field;
    }

    @Override
    public String toString(String field) {
        return "SpanMatchNoDocsQuery(\"" + reason + "\")";
    }

    @Override
    public boolean equals(Object o) {
        return sameClassAs(o);
    }

    @Override
    public int hashCode() {
        return classHash();
    }

    @Override
    public Weight createWeight(IndexSearcher searcher, ScoreMode scoreMode, float boost) throws IOException {
        return new MatchNoDocsQuery(reason).createWeight(searcher, scoreMode, boost);
    }

    @Override
    public void visit(QueryVisitor visitor) {
    }
}
