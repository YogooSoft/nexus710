# Nexus 710

**Nexus 710** is a next-generation distributed search engine based on Elasticsearch 7.10 (Apache 2.0 License) with deep kernel modifications.

It solves the critical limitations of ES in the AI era: **inefficient vector search**, **expensive storage**, and **inflexible business logic coupling**.

## Core Features

### AI Native — Neural Engine
- **Built-in HNSW index**: Deeply integrated into Lucene segment merge lifecycle, not a bolt-on plugin
- **RRF hybrid fusion**: Single query executes BM25 + vector search with Reciprocal Rank Fusion scoring
- **PQ/SQ quantization**: Off-heap vector compression supporting billion-scale vector search per node

### Compute-Storage Separation — Aero Storage
- **S3 storage backend**: Lucene segments persist directly to S3/OSS/MinIO at 1/10th the cost of SSD
- **Multi-level block cache**: LRU-based local SSD cache with < 10% performance penalty on cache hit
- **Replica sharing**: Cold data replicas share S3 storage paths — replica storage cost drops to zero

### Programmable Ranking — Precision Ranker
- **Native scoring plugins**: Write scoring operators in Rust/C++ via JNI for near-native performance
- **Real-time feature join**: Inject external features (user profiles, inventory, pricing) during fetch phase
- **Dynamic circuit breaker**: Per-query memory tracking with partial degradation instead of full failure

## Quick Start

### Prerequisites
- JDK 17+ (Eclipse Temurin recommended)
- Gradle 7.6+
- Docker 24+ (for integration tests)

### Build from Source

```bash
./gradlew assemble -Dbuild.snapshot=false
```

### Run with Docker Compose

```bash
docker compose up -d
curl http://localhost:9200
```

### Run Single Node

```bash
./gradlew run
```

## Architecture

```
Client Layer          REST API / SQL API / Hybrid Query DSL
                      ──────────────────────────────────────
Coordinator Layer     Query Parse → Plan → Route → Merge → Re-rank
                      ──────────────────────────────────────
Engine Layer          Neural Engine │ Aero Storage │ Precision Ranker
                      ──────────────────────────────────────
Core Layer            Nexus Core Engine (Lucene 9.x)
                      ──────────────────────────────────────
Storage Layer         Hot Tier (Local SSD) │ Cold Tier (S3/OSS/MinIO)
```

## Project Structure

```
nexus-710/
├── server/               # Node startup, cluster management (ES core)
├── nexus-neural/         # Neural Engine: vector indexing & hybrid search
│   ├── hnsw/             # HNSW index integration
│   ├── quantization/     # PQ/SQ vector compression
│   └── hybrid/           # RRF score fusion
├── nexus-aero/           # Aero Storage: compute-storage separation
│   ├── s3-backend/       # S3 read/write adapter
│   ├── block-cache/      # Local block cache
│   └── tiering/          # Hot/cold tiering policy
├── nexus-ranker/         # Precision Ranker: programmable ranking
│   ├── native-plugin/    # JNI + Rust scoring plugin framework
│   ├── feature-join/     # Real-time feature association
│   └── circuit-breaker/  # Dynamic circuit breaker
├── nexus-benchmark/      # Benchmark suite
├── libs/                 # Shared libraries
├── modules/              # Core bundled modules
├── plugins/              # Analysis plugins
└── distribution/         # Packaging & distribution
```

## Roadmap

- **Phase 0** (2026 Q2): Base preparation — ES 7.10 fork, Lucene 9.x upgrade, project restructure
- **Phase 1** (2026 Q2-Q3): Neural Engine — vector indexing, hybrid search, RRF fusion
- **Phase 2** (2026 Q3-Q4): Aero Storage — S3 backend, block cache, hot/cold tiering
- **Phase 3** (2026 Q4-2027 Q1): Precision Ranker — native scoring, feature join, SIMD optimization

## ES API Compatibility

Nexus 710 maintains 100% REST API compatibility with Elasticsearch 7.10. Existing clients and tools work without modification.

## License

Apache License 2.0. Based on Elasticsearch 7.10.2 (Apache 2.0). All improvements are Apache 2.0 — never going closed source.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.

---

Copyright © 2021 Elasticsearch B.V. Licensed under the Apache License, Version 2.0.

Copyright © 2026 Yogoo Software Co., Ltd. All modifications licensed under the Apache License, Version 2.0.

GitHub: [https://github.com/YogooSoft/nexus710](https://github.com/YogooSoft/nexus710)

Contact: [support@yogoo.net](mailto:support@yogoo.net)
