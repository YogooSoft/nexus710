/*
 * Licensed under the Apache License, Version 2.0
 * Based on Elasticsearch 7.10.2 (Apache 2.0)
 */

package io.nexus710.aero.s3;

import java.io.IOException;
import java.util.List;

/**
 * Abstraction layer for segment storage, supporting both local SSD and
 * remote object storage (S3/MinIO) backends.
 *
 * Stub for Phase 2 implementation.
 */
public interface SegmentStore {

    record SegmentHandle(String indexName, String segmentId, String storagePath) {}

    record SegmentMetadata(long sizeBytes, long createdAt, long lastAccessedAt, int accessCount) {}

    SegmentHandle writeSegment(String indexName, byte[] data) throws IOException;

    byte[] readBlock(SegmentHandle handle, long offset, int length) throws IOException;

    void deleteSegment(SegmentHandle handle) throws IOException;

    List<SegmentHandle> listSegments(String indexName) throws IOException;

    SegmentMetadata getMetadata(SegmentHandle handle) throws IOException;
}
