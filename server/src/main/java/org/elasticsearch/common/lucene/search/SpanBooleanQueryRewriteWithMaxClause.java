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

package org.elasticsearch.common.lucene.search;

// TODO: Lucene 9.x migration - spans removed
// This class previously extended SpanMultiTermQueryWrapper.SpanRewriteMethod.
// All span APIs have been removed in Lucene 9.x.
// Keeping as a stub so that references from other code (e.g. MatchQuery) compile
// after those references are also commented out.

import org.apache.lucene.search.BooleanQuery;

/*
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReaderContext;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.queries.SpanMatchNoDocsQuery;
import org.apache.lucene.search.MultiTermQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.spans.SpanMultiTermQueryWrapper;
import org.apache.lucene.search.spans.SpanOrQuery;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.apache.lucene.util.AttributeSource;
import org.apache.lucene.util.BytesRef;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
*/

/**
 * Stub class - previously a span rewrite method that extracted the first maxExpansions terms.
 * Span APIs were removed in Lucene 9.x.
 */
public class SpanBooleanQueryRewriteWithMaxClause {
    private final int maxExpansions;
    private final boolean hardLimit;

    public SpanBooleanQueryRewriteWithMaxClause() {
        this(BooleanQuery.getMaxClauseCount(), true);
    }

    public SpanBooleanQueryRewriteWithMaxClause(int maxExpansions, boolean hardLimit) {
        this.maxExpansions = maxExpansions;
        this.hardLimit = hardLimit;
    }

    public int getMaxExpansions() {
        return maxExpansions;
    }

    public boolean isHardLimit() {
        return hardLimit;
    }
}
