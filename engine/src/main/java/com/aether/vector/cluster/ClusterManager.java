package com.aether.vector.cluster;

import java.util.ArrayList;
import java.util.List;

import com.aether.vector.consensus.RaftNode.Peer;

/**
 * Tracks cluster membership and routes keys to shard owners via a {@link HashRing}.
 *
 * <p>TODO(Phase 4): gossip/Raft-backed membership, node join/leave, and shard rebalancing.
 */
public final class ClusterManager {

    public static final int DEFAULT_VIRTUAL_NODES = 128;

    private final Peer self;
    private final List<Peer> peers;
    private final HashRing<String> ring;

    public ClusterManager(Peer self, List<Peer> peers, int virtualNodes) {
        this.self = self;
        this.peers = List.copyOf(peers);
        this.ring = new HashRing<>(virtualNodes);
        ring.addNode(self.id());
        peers.forEach(p -> ring.addNode(p.id()));
    }

    /**
     * Parses a peer list of the form {@code id@host:port,id@host:port}. Blank input yields an empty list.
     */
    public static List<Peer> parsePeers(String spec) {
        List<Peer> result = new ArrayList<>();
        if (spec == null || spec.isBlank()) {
            return result;
        }
        for (String part : spec.split(",")) {
            String trimmed = part.trim();
            int at = trimmed.indexOf('@');
            int colon = trimmed.lastIndexOf(':');
            if (at <= 0 || colon <= at + 1 || colon == trimmed.length() - 1) {
                throw new IllegalArgumentException("Peer must look like id@host:port but was: " + trimmed);
            }
            result.add(new Peer(
                    trimmed.substring(0, at),
                    trimmed.substring(at + 1, colon),
                    Integer.parseInt(trimmed.substring(colon + 1))));
        }
        return result;
    }

    /** Id of the node that owns {@code key}. */
    public String ownerOf(String key) {
        return ring.nodeFor(key);
    }

    public boolean isLocal(String key) {
        return self.id().equals(ownerOf(key));
    }

    public Peer self() {
        return self;
    }

    public List<Peer> peers() {
        return peers;
    }
}
