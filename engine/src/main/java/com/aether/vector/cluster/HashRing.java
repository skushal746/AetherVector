package com.aether.vector.cluster;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * Consistent hash ring with virtual nodes. Each physical node is placed at {@code virtualNodes} points on a
 * 64-bit ring; a key is owned by the first point clockwise from its hash.
 *
 * @param <N> node identity type; its {@code toString()} seeds virtual-node positions, so it must be stable
 */
public final class HashRing<N> {

    private final int virtualNodes;
    private final NavigableMap<Long, N> ring = new ConcurrentSkipListMap<>();

    public HashRing(int virtualNodes) {
        if (virtualNodes <= 0) {
            throw new IllegalArgumentException("virtualNodes must be positive");
        }
        this.virtualNodes = virtualNodes;
    }

    public synchronized void addNode(N node) {
        for (int i = 0; i < virtualNodes; i++) {
            ring.put(hash(node + "#" + i), node);
        }
    }

    public synchronized void removeNode(N node) {
        for (int i = 0; i < virtualNodes; i++) {
            ring.remove(hash(node + "#" + i), node);
        }
    }

    /** The node owning {@code key}. */
    public N nodeFor(String key) {
        if (ring.isEmpty()) {
            throw new IllegalStateException("Hash ring is empty");
        }
        Map.Entry<Long, N> entry = ring.ceilingEntry(hash(key));
        return entry != null ? entry.getValue() : ring.firstEntry().getValue();
    }

    /** Up to {@code count} distinct nodes walking clockwise from {@code key} (primary first) — the replica set. */
    public List<N> nodesFor(String key, int count) {
        if (ring.isEmpty()) {
            throw new IllegalStateException("Hash ring is empty");
        }
        Set<N> result = new LinkedHashSet<>();
        long h = hash(key);
        for (N node : ring.tailMap(h, true).values()) {
            if (result.size() == count) {
                break;
            }
            result.add(node);
        }
        for (N node : ring.headMap(h, false).values()) {
            if (result.size() == count) {
                break;
            }
            result.add(node);
        }
        return new ArrayList<>(result);
    }

    public Set<N> nodes() {
        return new LinkedHashSet<>(ring.values());
    }

    public boolean isEmpty() {
        return ring.isEmpty();
    }

    static long hash(String key) {
        try {
            byte[] digest = MessageDigest.getInstance("MD5").digest(key.getBytes(StandardCharsets.UTF_8));
            long h = 0;
            for (int i = 0; i < Long.BYTES; i++) {
                h = (h << 8) | (digest[i] & 0xFF);
            }
            return h;
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError("MD5 is mandated by the JDK", e);
        }
    }
}
