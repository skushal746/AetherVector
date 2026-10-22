package com.aether.vector.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.Random;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

class VectorMathTest {

    private static final float TOLERANCE = 1e-3f;

    // Lengths chosen to exercise pure-tail, exact-multiple and mixed SIMD/tail paths.
    @ParameterizedTest
    @ValueSource(ints = {1, 3, 7, 8, 16, 17, 128, 384, 1000, 1536})
    void simdMatchesScalarOnHeap(int dimension) {
        Random random = new Random(dimension);
        float[] a = randomVector(random, dimension);
        float[] b = randomVector(random, dimension);

        assertThat(VectorMath.dotProduct(a, b))
                .isCloseTo(VectorMath.scalarDotProduct(a, b), within(TOLERANCE * dimension));
        assertThat(VectorMath.cosineSimilarity(a, b))
                .isCloseTo(VectorMath.scalarCosineSimilarity(a, b), within(TOLERANCE));
        assertThat(VectorMath.l2DistanceSquared(a, b))
                .isCloseTo(VectorMath.scalarL2DistanceSquared(a, b), within(TOLERANCE * dimension));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 7, 16, 33, 768})
    void offHeapMatchesHeap(int dimension) {
        Random random = new Random(dimension);
        float[] a = randomVector(random, dimension);
        float[] b = randomVector(random, dimension);

        try (Arena arena = Arena.ofConfined()) {
            MemorySegment sa = arena.allocateArray(ValueLayout.JAVA_FLOAT, a);
            MemorySegment sb = arena.allocateArray(ValueLayout.JAVA_FLOAT, b);

            assertThat(VectorMath.dotProduct(sa, sb, dimension))
                    .isCloseTo(VectorMath.dotProduct(a, b), within(TOLERANCE * dimension));
            assertThat(VectorMath.cosineSimilarity(sa, sb, dimension))
                    .isCloseTo(VectorMath.cosineSimilarity(a, b), within(TOLERANCE));
            assertThat(VectorMath.l2DistanceSquared(sa, sb, dimension))
                    .isCloseTo(VectorMath.l2DistanceSquared(a, b), within(TOLERANCE * dimension));
        }
    }

    @Test
    void knownValues() {
        float[] x = {1, 0, 0};
        float[] y = {0, 1, 0};
        assertThat(VectorMath.cosineSimilarity(x, x)).isCloseTo(1f, within(1e-6f));
        assertThat(VectorMath.cosineSimilarity(x, y)).isCloseTo(0f, within(1e-6f));
        assertThat(VectorMath.l2DistanceSquared(x, y)).isCloseTo(2f, within(1e-6f));
    }

    @Test
    void zeroVectorHasZeroCosine() {
        assertThat(VectorMath.cosineSimilarity(new float[4], new float[] {1, 2, 3, 4})).isZero();
    }

    @Test
    void rejectsMismatchedDimensions() {
        assertThatThrownBy(() -> VectorMath.dotProduct(new float[3], new float[4]))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static float[] randomVector(Random random, int dimension) {
        float[] v = new float[dimension];
        for (int i = 0; i < dimension; i++) {
            v[i] = random.nextFloat() * 2 - 1;
        }
        return v;
    }
}
