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

package org.elasticsearch.index.query;

import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
// TODO: Lucene 9.x removed lucene-spatial-extras (prefix tree support)
// import org.apache.lucene.spatial.prefix.PrefixTreeStrategy;
// import org.apache.lucene.spatial.prefix.RecursivePrefixTreeStrategy;
// import org.apache.lucene.spatial.query.SpatialArgs;
// import org.apache.lucene.spatial.query.SpatialOperation;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.common.geo.ShapeRelation;
import org.elasticsearch.common.geo.SpatialStrategy;
import org.elasticsearch.common.geo.builders.CircleBuilder;
import org.elasticsearch.common.geo.builders.EnvelopeBuilder;
import org.elasticsearch.common.geo.builders.GeometryCollectionBuilder;
import org.elasticsearch.common.geo.builders.LineStringBuilder;
import org.elasticsearch.common.geo.builders.MultiLineStringBuilder;
import org.elasticsearch.common.geo.builders.MultiPointBuilder;
import org.elasticsearch.common.geo.builders.MultiPolygonBuilder;
import org.elasticsearch.common.geo.builders.PointBuilder;
import org.elasticsearch.common.geo.builders.PolygonBuilder;
import org.elasticsearch.common.geo.builders.ShapeBuilder;
import org.elasticsearch.common.unit.DistanceUnit;
import org.elasticsearch.geometry.Circle;
import org.elasticsearch.geometry.Geometry;
import org.elasticsearch.geometry.GeometryCollection;
import org.elasticsearch.geometry.GeometryVisitor;
import org.elasticsearch.geometry.Line;
import org.elasticsearch.geometry.LinearRing;
import org.elasticsearch.geometry.MultiLine;
import org.elasticsearch.geometry.MultiPoint;
import org.elasticsearch.geometry.MultiPolygon;
import org.elasticsearch.geometry.Point;
import org.elasticsearch.geometry.Polygon;
import org.elasticsearch.geometry.Rectangle;
import org.elasticsearch.index.mapper.AbstractShapeGeometryFieldMapper;
import org.elasticsearch.index.mapper.LegacyGeoShapeFieldMapper;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.spatial4j.shape.Shape;

import java.util.ArrayList;
import java.util.List;

import static org.elasticsearch.search.SearchService.ALLOW_EXPENSIVE_QUERIES;

public class LegacyGeoShapeQueryProcessor  {

    private AbstractShapeGeometryFieldMapper.AbstractShapeGeometryFieldType ft;

    public LegacyGeoShapeQueryProcessor(AbstractShapeGeometryFieldMapper.AbstractShapeGeometryFieldType ft) {
        this.ft = ft;
    }

    public Query geoShapeQuery(Geometry shape, String fieldName, SpatialStrategy strategy,
                               ShapeRelation relation, QueryShardContext context) {
        throw new UnsupportedOperationException("Legacy geo shape with spatial prefix tree not supported in Lucene 9.x");
    }

    public static Object getArgs(Geometry shape, ShapeRelation relation) {
        throw new UnsupportedOperationException("Legacy geo shape with spatial prefix tree not supported in Lucene 9.x");
    }

    /**
     * Builds JTS shape from a geometry
     * <p>
     * This method is needed to handle legacy indices and will be removed when we no longer need to build JTS shapes
     */
    private static Shape buildS4J(Geometry geometry) {
        return geometryToShapeBuilder(geometry).buildS4J();
    }

    public static ShapeBuilder<?, ?, ?> geometryToShapeBuilder(Geometry geometry) {
        ShapeBuilder<?, ?, ?> shapeBuilder = geometry.visit(new GeometryVisitor<ShapeBuilder<?, ?, ?>, RuntimeException>() {
            @Override
            public ShapeBuilder<?, ?, ?> visit(Circle circle) {
                return new CircleBuilder().center(circle.getLon(), circle.getLat()).radius(circle.getRadiusMeters(), DistanceUnit.METERS);
            }

            @Override
            public ShapeBuilder<?, ?, ?> visit(GeometryCollection<?> collection) {
                GeometryCollectionBuilder shapes = new GeometryCollectionBuilder();
                for (Geometry geometry : collection) {
                    shapes.shape(geometry.visit(this));
                }
                return shapes;
            }

            @Override
            public ShapeBuilder<?, ?, ?> visit(Line line) {
                List<Coordinate> coordinates = new ArrayList<>();
                for (int i = 0; i < line.length(); i++) {
                    coordinates.add(new Coordinate(line.getX(i), line.getY(i), line.getZ(i)));
                }
                return new LineStringBuilder(coordinates);
            }

            @Override
            public ShapeBuilder<?, ?, ?> visit(LinearRing ring) {
                throw new UnsupportedOperationException("circle is not supported");
            }

            @Override
            public ShapeBuilder<?, ?, ?> visit(MultiLine multiLine) {
                MultiLineStringBuilder lines = new MultiLineStringBuilder();
                for (int i = 0; i < multiLine.size(); i++) {
                    lines.linestring((LineStringBuilder) visit(multiLine.get(i)));
                }
                return lines;
            }

            @Override
            public ShapeBuilder<?, ?, ?> visit(MultiPoint multiPoint) {
                List<Coordinate> coordinates = new ArrayList<>();
                for (int i = 0; i < multiPoint.size(); i++) {
                    Point p = multiPoint.get(i);
                    coordinates.add(new Coordinate(p.getX(), p.getY(), p.getZ()));
                }
                return new MultiPointBuilder(coordinates);
            }

            @Override
            public ShapeBuilder<?, ?, ?> visit(MultiPolygon multiPolygon) {
                MultiPolygonBuilder polygons = new MultiPolygonBuilder();
                for (int i = 0; i < multiPolygon.size(); i++) {
                    polygons.polygon((PolygonBuilder) visit(multiPolygon.get(i)));
                }
                return polygons;
            }

            @Override
            public ShapeBuilder<?, ?, ?> visit(Point point) {
                return new PointBuilder(point.getX(), point.getY());
            }

            @Override
            public ShapeBuilder<?, ?, ?> visit(Polygon polygon) {
                PolygonBuilder polygonBuilder =
                    new PolygonBuilder((LineStringBuilder) visit((Line) polygon.getPolygon()),
                        ShapeBuilder.Orientation.RIGHT, false);
                for (int i = 0; i < polygon.getNumberOfHoles(); i++) {
                    polygonBuilder.hole((LineStringBuilder) visit((Line) polygon.getHole(i)));
                }
                return polygonBuilder;
            }

            @Override
            public ShapeBuilder<?, ?, ?> visit(Rectangle rectangle) {
                return new EnvelopeBuilder(new Coordinate(rectangle.getMinX(), rectangle.getMaxY()),
                    new Coordinate(rectangle.getMaxX(), rectangle.getMinY()));
            }
        });
        return shapeBuilder;
    }
}
