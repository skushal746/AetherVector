package com.aether.bench;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import com.aether.vector.core.VectorMath;

/** SIMD (Vector API) vs scalar loop latency for the core distance kernels. */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(value = 1, jvmArgsAppend = {"--enable-preview", "--add-modules=jdk.incubator.vector"})
@State(Scope.Thread)
public class DistanceBenchmark {

    @Param({"128", "768", "1536"})
    int dimension;

    float[] a;
    float[] b;

    @Setup
    public void setup() {
        Random random = new Random(42);
        a = new float[dimension];
        b = new float[dimension];
        for (int i = 0; i < dimension; i++) {
            a[i] = random.nextFloat();
            b[i] = random.nextFloat();
        }
    }

    @Benchmark
    public float cosineSimd() {
        return VectorMath.cosineSimilarity(a, b);
    }

    @Benchmark
    public float cosineScalar() {
        return VectorMath.scalarCosineSimilarity(a, b);
    }

    @Benchmark
    public float l2Simd() {
        return VectorMath.l2DistanceSquared(a, b);
    }

    @Benchmark
    public float l2Scalar() {
        return VectorMath.scalarL2DistanceSquared(a, b);
    }
}
