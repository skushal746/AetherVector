package com.aether.vector.core;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Fixed-capacity, fixed-dimension vector store backed by a single contiguous off-heap {@link MemorySegment}.
 *
 * <p>Vectors are addressed by a dense integer id ({@code 0..size-1}) and laid out back-to-back, so vector
 * {@code id} lives at byte offset {@code id * dimension * 4}. The block is 64-byte aligned so SIMD loads
 * line up with cache lines. Memory is owned by a shared {@link Arena} and released on {@link #close()};
 * none of it is visible to the garbage collector.
 *
 * <p>Thread-safety: id allocation is atomic and distinct ids map to disjoint slices, so concurrent
 * {@link #add} calls are safe. Readers must only access ids returned by a completed {@code add}.
 */
public final class OffHeapVectorStore implements AutoCloseable {

    private static final long ALIGNMENT = 64;
    private static final ValueLayout.OfFloat FLOAT = ValueLayout.JAVA_FLOAT_UNALIGNED;

    private final int dimension;
    private final int capacity;
    private final long vectorBytes;
    private final Arena arena;
    private final MemorySegment block;
    private final AtomicInteger size = new AtomicInteger();

    public OffHeapVectorStore(int dimension, int capacity) {
        if (dimension <= 0) {
            throw new IllegalArgumentException("dimension must be positive: " + dimension);
        }
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive: " + capacity);
        }
        this.dimension = dimension;
        this.capacity = capacity;
        this.vectorBytes = (long) dimension * Float.BYTES;
        this.arena = Arena.ofShared();
        this.block = arena.allocate(vectorBytes * capacity, ALIGNMENT);
    }

    /** Appends a vector and returns its id. */
    public int add(float[] vector) {
        checkDimension(vector);
        int id = size.getAndIncrement();
        if (id >= capacity) {
            size.decrementAndGet();
            throw new IllegalStateException("Vector store is full (capacity " + capacity + ")");
        }
        write(id, vector);
        return id;
    }

    /** Overwrites the vector stored under an existing id. */
    public void set(int id, float[] vector) {
        checkDimension(vector);
        checkId(id);
        write(id, vector);
    }

    /** Copies the vector out to the heap. Prefer {@link #segment(int)} on hot paths. */
    public float[] get(int id) {
        checkId(id);
        return segment(id).toArray(FLOAT);
    }

    /** Zero-copy view of a single vector's bytes. */
    public MemorySegment segment(int id) {
        checkId(id);
        return block.asSlice(id * vectorBytes, vectorBytes);
    }

    /** Distance between two stored vectors, computed entirely off-heap. */
    public float distance(int a, int b, DistanceMetric metric) {
        return metric.distance(segment(a), segment(b), dimension);
    }

    /** Distance between a stored vector and a query held in a (possibly heap-backed) segment. */
    public float distance(int id, MemorySegment query, DistanceMetric metric) {
        return metric.distance(segment(id), query, dimension);
    }

    public int dimension() {
        return dimension;
    }

    public int capacity() {
        return capacity;
    }

    public int size() {
        return Math.min(size.get(), capacity);
    }

    /** Total off-heap bytes reserved by this store. */
    public long reservedBytes() {
        return block.byteSize();
    }

    @Override
    public void close() {
        arena.close();
    }

    private void write(int id, float[] vector) {
        MemorySegment.copy(vector, 0, block, FLOAT, id * vectorBytes, dimension);
    }

    private void checkDimension(float[] vector) {
        if (vector.length != dimension) {
            throw new IllegalArgumentException("Expected dimension " + dimension + " but got " + vector.length);
        }
    }

    private void checkId(int id) {
        if (id < 0 || id >= size()) {
            throw new IndexOutOfBoundsException("Unknown vector id " + id + " (size " + size() + ")");
        }
    }
}
