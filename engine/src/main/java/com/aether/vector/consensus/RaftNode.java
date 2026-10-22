package com.aether.vector.consensus;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aether.vector.consensus.rpc.proto.AppendEntriesRequest;
import com.aether.vector.consensus.rpc.proto.AppendEntriesResponse;
import com.aether.vector.consensus.rpc.proto.RequestVoteRequest;
import com.aether.vector.consensus.rpc.proto.RequestVoteResponse;

/**
 * Single member of a Raft group replicating one vector shard.
 *
 * <p>TODO(Phase 3): randomized election timers, vote handling with log up-to-date checks, leader
 * heartbeats, log replication / commit index advancement, and applying committed entries to the shard.
 */
public final class RaftNode implements AutoCloseable {

    public enum Role { FOLLOWER, CANDIDATE, LEADER }

    /** A peer in the same Raft group. */
    public record Peer(String id, String host, int port) {
    }

    private static final Logger log = LoggerFactory.getLogger(RaftNode.class);

    private final String id;
    private final List<Peer> peers;
    private final WriteAheadLog wal;

    private volatile Role role = Role.FOLLOWER;
    private volatile long currentTerm;
    private volatile String votedFor;

    public RaftNode(String id, List<Peer> peers, WriteAheadLog wal) {
        this.id = id;
        this.peers = List.copyOf(peers);
        this.wal = wal;
    }

    public void start() {
        log.info("Raft node {} starting as {} (term {}) with peers {}", id, role, currentTerm, peers);
    }

    public RequestVoteResponse handleRequestVote(RequestVoteRequest request) {
        throw new UnsupportedOperationException("RequestVote is scheduled for Phase 3");
    }

    public AppendEntriesResponse handleAppendEntries(AppendEntriesRequest request) {
        throw new UnsupportedOperationException("AppendEntries is scheduled for Phase 3");
    }

    public String id() {
        return id;
    }

    public Role role() {
        return role;
    }

    public long currentTerm() {
        return currentTerm;
    }

    public String votedFor() {
        return votedFor;
    }

    public List<Peer> peers() {
        return peers;
    }

    @Override
    public void close() {
        wal.close();
    }
}
