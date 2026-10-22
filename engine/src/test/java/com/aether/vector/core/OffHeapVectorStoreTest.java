package com.aether.vector.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

class OffHeapVectorStoreTest {

    @Test
    void storesAndReadsBackVectors() {
        try (OffHeapVectorStore store = new OffHeapVectorStore(3, 4)) {
            int a = store.add(new float[] {1, 2, 3});
            int b = store.add(new float[] {4, 5, 6});

            assertThat(store.size()).isEqualTo(2);
            assertThat(store.get(a)).containsExactly(1, 2, 3);
            assertThat(store.get(b)).containsExactly(4, 5, 6);
            assertThat(store.reservedBytes()).isEqualTo(3L * 4 * Float.BYTES);
        }
    }

    @Test
    void overwritesInPlace() {
        try (OffHeapVectorStore store = new OffHeapVectorStore(2, 1)) {
            int id = store.add(new float[] {1, 1});
            store.set(id, new float[] {9, 8});
            assertThat(store.get(id)).containsExactly(9, 8);
        }
    }

    @Test
    void computesDistancesOffHeap() {
        try (OffHeapVectorStore store = new OffHeapVectorStore(2, 2)) {
            int a = store.add(new float[] {0, 0});
            int b = store.add(new float[] {3, 4});
            assertThat(store.distance(a, b, DistanceMetric.L2)).isCloseTo(25f, within(1e-5f));
        }
    }

    @Test
    void rejectsInvalidInput() {
        try (OffHeapVectorStore store = new OffHeapVectorStore(2, 1)) {
            assertThatThrownBy(() -> store.add(new float[3])).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> store.get(0)).isInstanceOf(IndexOutOfBoundsException.class);
            store.add(new float[2]);
            assertThatThrownBy(() -> store.add(new float[2])).isInstanceOf(IllegalStateException.class);
            assertThat(store.size()).isEqualTo(1);
        }
    }

    @Test
    void concurrentAddsGetDistinctIds() {
        int count = 10_000;
        try (OffHeapVectorStore store = new OffHeapVectorStore(8, count)) {
            Set<Integer> ids = ConcurrentHashMap.newKeySet();
            IntStream.range(0, count).parallel().forEach(i -> {
                float[] v = new float[8];
                v[0] = i;
                ids.add(store.add(v));
            });
            assertThat(ids).hasSize(count);
            assertThat(store.size()).isEqualTo(count);
        }
    }
}
