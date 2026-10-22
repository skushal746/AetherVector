package com.aether.vector.index;

import java.util.concurrent.atomic.AtomicIntegerArray;

/**
 * A vertex in the HNSW graph. The vector payload itself lives in the
 * {@link com.aether.vector.core.OffHeapVectorStore}; the node only keeps its id and per-layer adjacency.
 *
 * <p>Neighbor lists are fixed-size {@link AtomicIntegerArray}s (slot value {@code -1} = empty) so that
 * Phase 2 can link nodes concurrently without taking a global lock.
 */
public final class HnswNode {

    public static final int EMPTY = -1;

    private final int id;
    private final int level;
    private final AtomicIntegerArray[] neighbors;

    /**
     * @param id            vector id in the backing store
     * @param level         top layer this node participates in (0 = base layer only)
     * @param maxConnections neighbor slots on layers above 0 (M)
     * @param maxConnections0 neighbor slots on layer 0 (typically 2*M)
     */
    public HnswNode(int id, int level, int maxConnections, int maxConnections0) {
        this.id = id;
        this.level = level;
        this.neighbors = new AtomicIntegerArray[level + 1];
        for (int layer = 0; layer <= level; layer++) {
            int slots = layer == 0 ? maxConnections0 : maxConnections;
            AtomicIntegerArray list = new AtomicIntegerArray(slots);
            for (int i = 0; i < slots; i++) {
                list.set(i, EMPTY);
            }
            neighbors[layer] = list;
        }
    }

    public int id() {
        return id;
    }

    public int level() {
        return level;
    }

    public AtomicIntegerArray neighbors(int layer) {
        return neighbors[layer];
    }
}
