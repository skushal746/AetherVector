package com.aether.vector;

import java.nio.file.Path;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aether.vector.cluster.ClusterManager;
import com.aether.vector.consensus.RaftNode;
import com.aether.vector.consensus.RaftNode.Peer;
import com.aether.vector.consensus.WriteAheadLog;
import com.aether.vector.consensus.rpc.RaftRpcServer;
import com.aether.vector.core.VectorMath;

/**
 * Process entry point for a single AetherVector cluster node.
 *
 * <p>Configured through environment variables:
 * <ul>
 *   <li>{@code AETHER_NODE_ID} – unique node id (default {@code node-1})</li>
 *   <li>{@code AETHER_HOST} – advertised host (default {@code localhost})</li>
 *   <li>{@code AETHER_RPC_PORT} – Raft gRPC port (default {@code 9090})</li>
 *   <li>{@code AETHER_PEERS} – other members as {@code id@host:port,...} (default none)</li>
 *   <li>{@code AETHER_DATA_DIR} – WAL directory (default {@code ./data})</li>
 * </ul>
 */
public final class AetherVectorNode {

    private static final Logger log = LoggerFactory.getLogger(AetherVectorNode.class);

    private AetherVectorNode() {
    }

    public static void main(String[] args) throws Exception {
        String nodeId = env("AETHER_NODE_ID", "node-1");
        String host = env("AETHER_HOST", "localhost");
        int port = Integer.parseInt(env("AETHER_RPC_PORT", "9090"));
        List<Peer> peers = ClusterManager.parsePeers(env("AETHER_PEERS", ""));
        Path dataDir = Path.of(env("AETHER_DATA_DIR", "./data"), nodeId);

        Peer self = new Peer(nodeId, host, port);
        new ClusterManager(self, peers, ClusterManager.DEFAULT_VIRTUAL_NODES);

        log.info("Starting AetherVector node {} ({} SIMD float lanes, {} peers)",
                nodeId, VectorMath.laneCount(), peers.size());

        RaftNode raft = new RaftNode(nodeId, peers, new WriteAheadLog(dataDir));
        RaftRpcServer server = new RaftRpcServer(port, raft).start();
        raft.start();

        Runtime.getRuntime().addShutdownHook(Thread.ofPlatform().unstarted(() -> {
            log.info("Shutting down node {}", nodeId);
            try {
                server.close();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            raft.close();
        }));

        server.awaitTermination();
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }
}
