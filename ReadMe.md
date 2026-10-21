AetherVector

A High-Throughput, Off-Heap, Sharded Vector Engine with Raft Consensus in Modern Java 21

AetherVector is an enterprise-grade, distributed vector database engineered from first principles in Java 21. Designed specifically to bypass JVM Garbage Collection overhead during large-scale embedding search, AetherVector combines unmanaged off-heap memory storage (Foreign Function & Memory API), hardware-accelerated SIMD calculations (Vector API), hierarchical approximate nearest neighbor search (HNSW), and strong consistency via an embedded Raft consensus engine.

🌟 Key Technical Innovations

Zero-GC Off-Heap Storage Engine: Allocates and manages contiguous memory blocks off-heap using Java 21's Foreign Function & Memory API (Arena & MemorySegment), eliminating Stop-The-World (STW) GC pauses under multi-gigabyte vector indexing.

SIMD Vector Math Acceleration: Executes single-instruction multiple-data (SIMD) Cosine Similarity and L2 Euclidean Distance calculations across AVX-512 / ARM NEON registers using jdk.incubator.vector.FloatVector.

Lock-Free HNSW Graph Index: Implements multi-layer Hierarchical Navigable Small World (HNSW) graph indexing with concurrent node linkage for $O(\log N)$ approximate nearest neighbor ($k$-NN) retrieval.

Embedded Raft Consensus Engine: Guarantees single-leader strong consistency, linearizable writes, and Write-Ahead Log (WAL) replication across vector shards.

Consistent Hash Ring Routing: Automatically partitions vector space coordinates across cluster nodes using consistent hashing with virtual nodes.

🏗 System Architecture

graph TD
    Client[Client Application / REST / gRPC] --> HashRing[Consistent Hash Ring / Router]

    subgraph Cluster Node A [Shard Leader]
        HashRing -->|Vector Write / Search| RaftNodeA[Raft Engine / WAL]
        RaftNodeA -->|Apply Log| OffHeapStoreA[Off-Heap Memory Segment]
        OffHeapStoreA -->|SIMD Distance| HNSWIndexA[HNSW Graph Layers]
    end

    subgraph Cluster Node B [Shard Follower]
        RaftNodeA -->|AppendEntries RPC| RaftNodeB[Raft Engine]
        RaftNodeB --> OffHeapStoreB[Off-Heap Memory Segment]
        OffHeapStoreB --> HNSWIndexB[HNSW Graph Layers]
    end

    subgraph Hardware Acceleration Layer
        HNSWIndexA -->|SIMD Execution| CPU[AVX-512 / ARM NEON Registers]
    end


📁 Repository Structure

aether-vector/
├── pom.xml                                  # Java 21 Preview & Incubator Compiler Flags
├── README.md
├── docker-compose.yml                       # 3-Node Local Cluster Setup
├── benchmarks/                              # JMH (Java Microbenchmark Harness) Suite
│   └── src/main/java/com/aether/bench/
│       ├── DistanceBenchmark.java           # SIMD vs Scalar Loop Latency
│       └── AllocationBenchmark.java         # Off-Heap vs On-Heap Allocation
└── src/
    ├── main/
    │   ├── java/com/aether/vector/
    │   │   ├── core/                        # Off-Heap Memory & SIMD Math
    │   │   │   ├── OffHeapVectorStore.java
    │   │   │   └── VectorMath.java
    │   │   ├── index/                       # HNSW Algorithm Implementation
    │   │   │   ├── HnswGraph.java
    │   │   │   ├── HnswNode.java
    │   │   │   └── HnswSearcher.java
    │   │   ├── consensus/                   # Raft Engine Implementation
    │   │   │   ├── RaftNode.java
    │   │   │   ├── WriteAheadLog.java
    │   │   │   └── rpc/
    │   │   └── cluster/                     # Distributed Sharding
    │   │       ├── HashRing.java
    │   │       └── ClusterManager.java
    │   └── proto/                           # Protobuf RPC Definitions
    │       └── raft_service.proto
    └── test/
        └── java/com/aether/vector/          # JUnit 5 & Testcontainers Suite


🛠 Prerequisites & Build Configuration

Requirements

JDK: OpenJDK 21 or higher (GraalVM Enterprise CE 21+ recommended)

Build Tool: Apache Maven 3.9+

CPU: x86_64 with AVX2/AVX-512 or Apple Silicon (ARM v8.2+ NEON)

Compiler Flags

Because this engine leverages preview and incubator features, the compiler requires specific modules:

<!-- Maven Compiler Plugin Snippet -->
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <version>3.11.0</version>
    <configuration>
        <release>21</release>
        <compilerArgs>
            <arg>--enable-preview</arg>
            <arg>--add-modules=jdk.incubator.vector</arg>
        </compilerArgs>
    </configuration>
</plugin>


🚀 Quick Start Guide

1. Clone & Build

git clone https://github.com/skushal746/aether-vector.git
cd aether-vector
mvn clean package -DskipTests


2. Run SIMD vs Scalar Performance Benchmarks

Verify SIMD acceleration on your native hardware architecture using JMH:

java --enable-preview \
     --add-modules=jdk.incubator.vector \
     -jar benchmarks/target/benchmarks.jar DistanceBenchmark


3. Spin Up a 3-Node Raft Cluster

Deploy a local 3-node distributed vector cluster using Docker Compose:

docker-compose up -d
docker-compose logs -f


⚡ Code Highlights

Hardware-Accelerated Vector Cosine Distance (VectorMath.java)

package com.aether.vector.core;

import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.VectorOperators;
import jdk.incubator.vector.VectorSpecies;

public class VectorMath {
    private static final VectorSpecies<Float> SPECIES = FloatVector.SPECIES_PREFERRED;

    public static float cosineSimilarity(float[] a, float[] b) {
        float dot = 0.0f, normA = 0.0f, normB = 0.0f;
        int i = 0;
        int upperBound = SPECIES.loopBound(a.length);

        FloatVector vDot = FloatVector.zero(SPECIES);
        FloatVector vNormA = FloatVector.zero(SPECIES);
        FloatVector vNormB = FloatVector.zero(SPECIES);

        for (; i < upperBound; i += SPECIES.length()) {
            FloatVector va = FloatVector.fromArray(SPECIES, a, i);
            FloatVector vb = FloatVector.fromArray(SPECIES, b, i);
            vDot = va.fma(vb, vDot);
            vNormA = va.fma(va, vNormA);
            vNormB = vb.fma(vb, vNormB);
        }

        dot = vDot.reduceLanes(VectorOperators.ADD);
        normA = vNormA.reduceLanes(VectorOperators.ADD);
        normB = vNormB.reduceLanes(VectorOperators.ADD);

        for (; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }

        return (float) (dot / (Math.sqrt(normA) * Math.sqrt(normB)));
    }
}


📅 Roadmap & Milestones

[ ] Phase 1: Core Off-Heap Memory Manager (MemorySegment) & SIMD Distance Harness.

[ ] Phase 2: Multi-Threaded HNSW Graph Indexing with recall validation ($>98\%$ recall@10).

[ ] Phase 3: Embedded Raft Consensus (Leader Election, Binary WAL, State Replication).

[ ] Phase 4: Consistent Hash Ring Routing, Node Join/Leave Rebalancing, & Jepsen Chaos Testing.

📜 License

Distributed under the MIT License. See LICENSE for details.

Maintained by: Kushal Sharma | Senior Software Engineer