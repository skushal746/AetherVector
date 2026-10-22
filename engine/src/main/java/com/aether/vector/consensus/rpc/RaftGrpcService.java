package com.aether.vector.consensus.rpc;

import com.aether.vector.consensus.RaftNode;
import com.aether.vector.consensus.rpc.proto.AppendEntriesRequest;
import com.aether.vector.consensus.rpc.proto.AppendEntriesResponse;
import com.aether.vector.consensus.rpc.proto.RaftServiceGrpc;
import com.aether.vector.consensus.rpc.proto.RequestVoteRequest;
import com.aether.vector.consensus.rpc.proto.RequestVoteResponse;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;

/** Adapts inbound gRPC calls onto a {@link RaftNode}. */
public final class RaftGrpcService extends RaftServiceGrpc.RaftServiceImplBase {

    private final RaftNode node;

    public RaftGrpcService(RaftNode node) {
        this.node = node;
    }

    @Override
    public void requestVote(RequestVoteRequest request, StreamObserver<RequestVoteResponse> responseObserver) {
        try {
            responseObserver.onNext(node.handleRequestVote(request));
            responseObserver.onCompleted();
        } catch (UnsupportedOperationException e) {
            responseObserver.onError(Status.UNIMPLEMENTED.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void appendEntries(AppendEntriesRequest request, StreamObserver<AppendEntriesResponse> responseObserver) {
        try {
            responseObserver.onNext(node.handleAppendEntries(request));
            responseObserver.onCompleted();
        } catch (UnsupportedOperationException e) {
            responseObserver.onError(Status.UNIMPLEMENTED.withDescription(e.getMessage()).asRuntimeException());
        }
    }
}
