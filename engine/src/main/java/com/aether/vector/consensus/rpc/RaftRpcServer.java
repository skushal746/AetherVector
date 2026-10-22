package com.aether.vector.consensus.rpc;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aether.vector.consensus.RaftNode;

import io.grpc.Server;
import io.grpc.ServerBuilder;

/** gRPC server exposing a node's {@code RaftService} to its peers. */
public final class RaftRpcServer implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(RaftRpcServer.class);

    private final Server server;

    public RaftRpcServer(int port, RaftNode node) {
        this.server = ServerBuilder.forPort(port)
                .addService(new RaftGrpcService(node))
                .build();
    }

    public RaftRpcServer start() throws IOException {
        server.start();
        log.info("Raft RPC server listening on port {}", server.getPort());
        return this;
    }

    public void awaitTermination() throws InterruptedException {
        server.awaitTermination();
    }

    @Override
    public void close() throws InterruptedException {
        server.shutdown();
        if (!server.awaitTermination(5, TimeUnit.SECONDS)) {
            server.shutdownNow();
        }
    }
}
