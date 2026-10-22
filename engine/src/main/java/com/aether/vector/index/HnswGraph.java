package com.aether.vector.index;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReference;

import com.aether.vector.core.DistanceMetric;
import com.aether.vector.core.OffHeapVectorStore;

/**
 * Multi-layer Hierarchical Navigable Small World graph over vectors held in an {@link OffHeapVectorStore}.
 *
 * <p>Phase 2 milestone: concurrent insertion with neighbor-selection heuristics and recall@10 &gt; 98%.
 */
public final class HnswGraph {

    /** Tunables from the HNSW paper (Malkov &amp; Yashunin, 2016). */
    public record Config(int m, int efConstruction, DistanceMetric metric) {
        public Config {
            if (m < 2) {
                throw new IllegalArgumentException("m must be >= 2");
            }
            if (efConstruction < m) {
                throw new IllegalArgumentException("efConstruction must be >= m");
            }
        }

        public static Config defaults(DistanceMetric metric) {
            return new Config(16, 200, metric);
        }

        int maxConnections0() {
            return 2 * m;
        }

        double levelMultiplier() {
            return 1.0 / Math.log(m);
        }
    }

    private final OffHeapVectorStore store;
    private final Config config;
    private final ConcurrentHashMap<Integer, HnswNode> nodes = new ConcurrentHashMap<>();
    private final AtomicReference<HnswNode> entryPoint = new AtomicReference<>();

    public HnswGraph(OffHeapVectorStore store, Config config) {
        this.store = store;
        this.config = config;
    }

    /**
     * Inserts an already-stored vector into the graph.
     *
     * <p>TODO(Phase 2): greedy descent from the entry point, ef-bounded search per layer, heuristic
     * neighbor selection, bidirectional linking with pruning, and entry-point promotion.
     */
    public void insert(int vectorId) {
        throw new UnsupportedOperationException("HNSW insertion is scheduled for Phase 2");
    }

    /** Draws a node level from the exponential distribution {@code floor(-ln(U) * mL)}. */
    int randomLevel() {
        double u = 1.0 - ThreadLocalRandom.current().nextDouble();
        return (int) Math.floor(-Math.log(u) * config.levelMultiplier());
    }

    HnswNode newNode(int vectorId) {
        return new HnswNode(vectorId, randomLevel(), config.m(), config.maxConnections0());
    }

    public HnswNode node(int vectorId) {
        return nodes.get(vectorId);
    }

    public HnswNode entryPoint() {
        return entryPoint.get();
    }

    public int size() {
        return nodes.size();
    }

    public OffHeapVectorStore store() {
        return store;
    }

    public Config config() {
        return config;
    }
}
