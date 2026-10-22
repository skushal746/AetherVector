package com.aether.vector.index;

import java.util.List;

/**
 * k-NN query execution against an {@link HnswGraph}.
 */
public final class HnswSearcher {

    /** A single search hit; {@code distance} follows the "smaller is closer" convention of the metric. */
    public record SearchResult(int vectorId, float distance) {
    }

    private final HnswGraph graph;

    public HnswSearcher(HnswGraph graph) {
        this.graph = graph;
    }

    /**
     * Returns the approximate {@code k} nearest neighbors of {@code query}, closest first.
     *
     * <p>TODO(Phase 2): greedy search on upper layers, then ef-bounded best-first search on layer 0.
     *
     * @param ef size of the dynamic candidate list; must be &gt;= k
     */
    public List<SearchResult> search(float[] query, int k, int ef) {
        if (query.length != graph.store().dimension()) {
            throw new IllegalArgumentException("Query dimension mismatch");
        }
        if (k <= 0 || ef < k) {
            throw new IllegalArgumentException("Require k > 0 and ef >= k");
        }
        throw new UnsupportedOperationException("HNSW search is scheduled for Phase 2");
    }
}
