package com.aether.vector.cluster;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.aether.vector.consensus.RaftNode.Peer;

class HashRingTest {

    @Test
    void routingIsDeterministic() {
        HashRing<String> ring = ringOf("a", "b", "c");
        String owner = ring.nodeFor("vector-42");
        for (int i = 0; i < 10; i++) {
            assertThat(ring.nodeFor("vector-42")).isEqualTo(owner);
        }
    }

    @Test
    void distributesKeysAcrossNodes() {
        HashRing<String> ring = ringOf("a", "b", "c");
        Map<String, Integer> counts = new HashMap<>();
        int keys = 30_000;
        for (int i = 0; i < keys; i++) {
            counts.merge(ring.nodeFor("key-" + i), 1, Integer::sum);
        }
        assertThat(counts).containsOnlyKeys("a", "b", "c");
        counts.values().forEach(c -> assertThat(c).isBetween(keys / 3 * 7 / 10, keys / 3 * 13 / 10));
    }

    @Test
    void removingANodeOnlyMovesItsKeys() {
        HashRing<String> ring = ringOf("a", "b", "c");
        Map<String, String> before = new HashMap<>();
        for (int i = 0; i < 5_000; i++) {
            before.put("key-" + i, ring.nodeFor("key-" + i));
        }
        ring.removeNode("c");
        before.forEach((key, owner) -> {
            if (!owner.equals("c")) {
                assertThat(ring.nodeFor(key)).isEqualTo(owner);
            }
        });
        assertThat(ring.nodes()).containsExactlyInAnyOrder("a", "b");
    }

    @Test
    void replicaSetHasDistinctNodesPrimaryFirst() {
        HashRing<String> ring = ringOf("a", "b", "c");
        List<String> replicas = ring.nodesFor("key-7", 3);
        assertThat(replicas).doesNotHaveDuplicates().hasSize(3);
        assertThat(replicas.getFirst()).isEqualTo(ring.nodeFor("key-7"));
        assertThat(ring.nodesFor("key-7", 5)).hasSize(3);
    }

    @Test
    void emptyRingRejectsLookups() {
        assertThatThrownBy(() -> new HashRing<String>(8).nodeFor("x")).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void parsesPeerSpec() {
        assertThat(ClusterManager.parsePeers("node-2@aether-2:9090, node-3@aether-3:9091"))
                .containsExactly(new Peer("node-2", "aether-2", 9090), new Peer("node-3", "aether-3", 9091));
        assertThat(ClusterManager.parsePeers("")).isEmpty();
        assertThatThrownBy(() -> ClusterManager.parsePeers("bad")).isInstanceOf(IllegalArgumentException.class);
    }

    private static HashRing<String> ringOf(String... nodes) {
        HashRing<String> ring = new HashRing<>(ClusterManager.DEFAULT_VIRTUAL_NODES);
        for (String node : nodes) {
            ring.addNode(node);
        }
        return ring;
    }
}
