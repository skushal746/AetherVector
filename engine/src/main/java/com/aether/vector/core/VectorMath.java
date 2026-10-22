package com.aether.vector.core;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.ByteOrder;

import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.VectorOperators;
import jdk.incubator.vector.VectorSpecies;

/**
 * SIMD-accelerated distance kernels built on the incubating Vector API.
 *
 * <p>Every kernel has a heap ({@code float[]}) and an off-heap ({@link MemorySegment}) variant, plus a
 * scalar reference implementation used by tests and benchmarks as a correctness/latency baseline.
 */
public final class VectorMath {

    private static final VectorSpecies<Float> SPECIES = FloatVector.SPECIES_PREFERRED;
    private static final ByteOrder ORDER = ByteOrder.nativeOrder();
    private static final ValueLayout.OfFloat FLOAT = ValueLayout.JAVA_FLOAT_UNALIGNED;

    private VectorMath() {
    }

    /** Number of float lanes processed per SIMD instruction on this CPU. */
    public static int laneCount() {
        return SPECIES.length();
    }

    // ---------------------------------------------------------------------------------------------
    // Heap arrays
    // ---------------------------------------------------------------------------------------------

    public static float dotProduct(float[] a, float[] b) {
        checkLengths(a.length, b.length);
        int i = 0;
        int upperBound = SPECIES.loopBound(a.length);
        FloatVector acc = FloatVector.zero(SPECIES);
        for (; i < upperBound; i += SPECIES.length()) {
            FloatVector va = FloatVector.fromArray(SPECIES, a, i);
            FloatVector vb = FloatVector.fromArray(SPECIES, b, i);
            acc = va.fma(vb, acc);
        }
        float dot = acc.reduceLanes(VectorOperators.ADD);
        for (; i < a.length; i++) {
            dot += a[i] * b[i];
        }
        return dot;
    }

    public static float cosineSimilarity(float[] a, float[] b) {
        checkLengths(a.length, b.length);
        int i = 0;
        int upperBound = SPECIES.loopBound(a.length);

        FloatVector vDot = FloatVector.zero(SPECIES);
        FloatVector vNormA = FloatVector.zero(SPECIES);
        FloatVector vNormB = FloatVector.zero(SPECIES);

        for (; i < upperBound; i += SPECIES.length()) {
            FloatVector va = FloatVector.fromArray(SPECIES, a, i);
            FloatVector vb = FloatVector.fromArray(SPECIES, b, i);
            vDot = va.fma(vb, vDot);
            vNormA = va.fma(va, vNormA);
            vNormB = vb.fma(vb, vNormB);
        }

        float dot = vDot.reduceLanes(VectorOperators.ADD);
        float normA = vNormA.reduceLanes(VectorOperators.ADD);
        float normB = vNormB.reduceLanes(VectorOperators.ADD);

        for (; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        return cosine(dot, normA, normB);
    }

    public static float l2DistanceSquared(float[] a, float[] b) {
        checkLengths(a.length, b.length);
        int i = 0;
        int upperBound = SPECIES.loopBound(a.length);
        FloatVector acc = FloatVector.zero(SPECIES);
        for (; i < upperBound; i += SPECIES.length()) {
            FloatVector diff = FloatVector.fromArray(SPECIES, a, i).sub(FloatVector.fromArray(SPECIES, b, i));
            acc = diff.fma(diff, acc);
        }
        float sum = acc.reduceLanes(VectorOperators.ADD);
        for (; i < a.length; i++) {
            float d = a[i] - b[i];
            sum += d * d;
        }
        return sum;
    }

    // ---------------------------------------------------------------------------------------------
    // Off-heap segments (each segment holds exactly `dimension` floats in native byte order)
    // ---------------------------------------------------------------------------------------------

    public static float dotProduct(MemorySegment a, MemorySegment b, int dimension) {
        int i = 0;
        int upperBound = SPECIES.loopBound(dimension);
        FloatVector acc = FloatVector.zero(SPECIES);
        for (; i < upperBound; i += SPECIES.length()) {
            long offset = (long) i * Float.BYTES;
            FloatVector va = FloatVector.fromMemorySegment(SPECIES, a, offset, ORDER);
            FloatVector vb = FloatVector.fromMemorySegment(SPECIES, b, offset, ORDER);
            acc = va.fma(vb, acc);
        }
        float dot = acc.reduceLanes(VectorOperators.ADD);
        for (; i < dimension; i++) {
            dot += floatAt(a, i) * floatAt(b, i);
        }
        return dot;
    }

    public static float cosineSimilarity(MemorySegment a, MemorySegment b, int dimension) {
        int i = 0;
        int upperBound = SPECIES.loopBound(dimension);
        FloatVector vDot = FloatVector.zero(SPECIES);
        FloatVector vNormA = FloatVector.zero(SPECIES);
        FloatVector vNormB = FloatVector.zero(SPECIES);
        for (; i < upperBound; i += SPECIES.length()) {
            long offset = (long) i * Float.BYTES;
            FloatVector va = FloatVector.fromMemorySegment(SPECIES, a, offset, ORDER);
            FloatVector vb = FloatVector.fromMemorySegment(SPECIES, b, offset, ORDER);
            vDot = va.fma(vb, vDot);
            vNormA = va.fma(va, vNormA);
            vNormB = vb.fma(vb, vNormB);
        }
        float dot = vDot.reduceLanes(VectorOperators.ADD);
        float normA = vNormA.reduceLanes(VectorOperators.ADD);
        float normB = vNormB.reduceLanes(VectorOperators.ADD);
        for (; i < dimension; i++) {
            float x = floatAt(a, i);
            float y = floatAt(b, i);
            dot += x * y;
            normA += x * x;
            normB += y * y;
        }
        return cosine(dot, normA, normB);
    }

    public static float l2DistanceSquared(MemorySegment a, MemorySegment b, int dimension) {
        int i = 0;
        int upperBound = SPECIES.loopBound(dimension);
        FloatVector acc = FloatVector.zero(SPECIES);
        for (; i < upperBound; i += SPECIES.length()) {
            long offset = (long) i * Float.BYTES;
            FloatVector diff = FloatVector.fromMemorySegment(SPECIES, a, offset, ORDER)
                    .sub(FloatVector.fromMemorySegment(SPECIES, b, offset, ORDER));
            acc = diff.fma(diff, acc);
        }
        float sum = acc.reduceLanes(VectorOperators.ADD);
        for (; i < dimension; i++) {
            float d = floatAt(a, i) - floatAt(b, i);
            sum += d * d;
        }
        return sum;
    }

    // ---------------------------------------------------------------------------------------------
    // Scalar reference implementations (baseline for tests and JMH)
    // ---------------------------------------------------------------------------------------------

    public static float scalarDotProduct(float[] a, float[] b) {
        checkLengths(a.length, b.length);
        float dot = 0f;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
        }
        return dot;
    }

    public static float scalarCosineSimilarity(float[] a, float[] b) {
        checkLengths(a.length, b.length);
        float dot = 0f, normA = 0f, normB = 0f;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        return cosine(dot, normA, normB);
    }

    public static float scalarL2DistanceSquared(float[] a, float[] b) {
        checkLengths(a.length, b.length);
        float sum = 0f;
        for (int i = 0; i < a.length; i++) {
            float d = a[i] - b[i];
            sum += d * d;
        }
        return sum;
    }

    // ---------------------------------------------------------------------------------------------

    private static float cosine(float dot, float normA, float normB) {
        if (normA == 0f || normB == 0f) {
            return 0f;
        }
        return (float) (dot / (Math.sqrt(normA) * Math.sqrt(normB)));
    }

    private static float floatAt(MemorySegment segment, int index) {
        return segment.getAtIndex(FLOAT, index);
    }

    private static void checkLengths(int a, int b) {
        if (a != b) {
            throw new IllegalArgumentException("Vector dimensions differ: " + a + " != " + b);
        }
    }
}
