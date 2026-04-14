/*
 * Licensed under the Apache License, Version 2.0
 * Based on Elasticsearch 7.10.2 (Apache 2.0)
 */

package io.nexus710.neural.quantization;

/**
 * Vector quantization interface for compressing high-dimensional vectors.
 * Supports Product Quantization (PQ) and Scalar Quantization (SQ).
 *
 * Stub for Phase 1 implementation.
 */
public interface VectorQuantizer {

    enum QuantizationType {
        PRODUCT,
        SCALAR
    }

    /**
     * Trains the quantizer on a set of training vectors.
     */
    void train(float[][] trainingVectors);

    /**
     * Encodes a vector into its quantized representation.
     */
    byte[] encode(float[] vector);

    /**
     * Decodes a quantized representation back to an approximate vector.
     */
    float[] decode(byte[] encoded);

    /**
     * Returns the compression ratio achieved by this quantizer.
     */
    float compressionRatio();
}
