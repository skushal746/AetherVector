package com.aether.vector.consensus;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import com.aether.vector.consensus.rpc.proto.LogEntry;

/**
 * Durable, append-only binary log of Raft entries.
 *
 * <p>TODO(Phase 3): segmented files with length-prefixed, CRC32C-checksummed records, fsync on commit,
 * crash recovery (truncate torn tail), and snapshot-driven compaction.
 */
public final class WriteAheadLog implements AutoCloseable {

    private final Path directory;

    public WriteAheadLog(Path directory) {
        this.directory = directory;
    }

    /** Appends entries and returns once they are durable. */
    public void append(List<LogEntry> entries) throws IOException {
        throw new UnsupportedOperationException("WAL is scheduled for Phase 3");
    }

    /** Returns entries in {@code [fromIndex, toIndexExclusive)}. */
    public List<LogEntry> read(long fromIndex, long toIndexExclusive) throws IOException {
        throw new UnsupportedOperationException("WAL is scheduled for Phase 3");
    }

    /** Removes every entry with index &gt;= {@code fromIndex} (conflict resolution on followers). */
    public void truncateFrom(long fromIndex) throws IOException {
        throw new UnsupportedOperationException("WAL is scheduled for Phase 3");
    }

    /** Index of the last entry, or 0 if the log is empty. */
    public long lastIndex() {
        return 0;
    }

    /** Term of the last entry, or 0 if the log is empty. */
    public long lastTerm() {
        return 0;
    }

    public Path directory() {
        return directory;
    }

    @Override
    public void close() {
    }
}
