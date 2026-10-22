package com.aether.vector.core;

import java.lang.foreign.MemorySegment;

/**
 * Distance functions exposed as "smaller is closer" so that index code can treat every metric uniformly.
 */
public enum DistanceMetric {

    COSINE {
        @Override
        public float distance(MemorySegment a, MemorySegment b, int dimension) {
            return 1f - VectorMath.cosineSimilarity(a, b, dimension);
        }

        @Override
        public float distance(float[] a, float[] b) {
            return 1f - VectorMath.cosineSimilarity(a, b);
        }
    },

    L2 {
        @Override
        public float distance(MemorySegment a, MemorySegment b, int dimension) {
            return VectorMath.l2DistanceSquared(a, b, dimension);
        }

        @Override
        public float distance(float[] a, float[] b) {
            return VectorMath.l2DistanceSquared(a, b);
        }
    },

    DOT_PRODUCT {
        @Override
        public float distance(MemorySegment a, MemorySegment b, int dimension) {
            return -VectorMath.dotProduct(a, b, dimension);
        }

        @Override
        public float distance(float[] a, float[] b) {
            return -VectorMath.dotProduct(a, b);
        }
    };

    public abstract float distance(MemorySegment a, MemorySegment b, int dimension);

    public abstract float distance(float[] a, float[] b);
}
