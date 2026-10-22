package com.aether.bench;

import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

import com.aether.vector.core.OffHeapVectorStore;

/** Cost of materialising N vectors in off-heap segments vs on-heap {@code float[][]}. */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(value = 1, jvmArgsAppend = {"--enable-preview", "--add-modules=jdk.incubator.vector"})
@State(Scope.Benchmark)
public class AllocationBenchmark {

    @Param({"768"})
    int dimension;

    @Param({"10000"})
    int count;

    @Benchmark
    public void offHeap(Blackhole bh) {
        float[] vector = new float[dimension];
        try (OffHeapVectorStore store = new OffHeapVectorStore(dimension, count)) {
            for (int i = 0; i < count; i++) {
                vector[0] = i;
                bh.consume(store.add(vector));
            }
        }
    }

    @Benchmark
    public float[][] onHeap() {
        float[][] vectors = new float[count][];
        for (int i = 0; i < count; i++) {
            float[] v = new float[dimension];
            v[0] = i;
            vectors[i] = v;
        }
        return vectors;
    }
}
