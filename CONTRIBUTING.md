# Contributing to Nexus 710

Thank you for considering contributing to Nexus 710. This document provides guidelines and conventions to help you get started.

## Getting Started

### Development Environment

| Requirement | Version |
|-------------|---------|
| JDK | 17+ (Eclipse Temurin recommended) |
| Gradle | 7.6+ (wrapper included) |
| Rust | 1.75+ (for native-plugin module) |
| Docker | 24+ (for integration tests) |
| MinIO | Latest (local S3-compatible testing) |
| Python | 3.10+ (benchmark scripts) |

### Building

```bash
# Full build
./gradlew assemble -Dbuild.snapshot=false

# Run tests
./gradlew test -Dbuild.snapshot=false

# Run a single node
./gradlew run

# Build specific module
./gradlew :nexus-neural:hnsw:build
```

### Running Tests

```bash
# Unit tests only
./gradlew test

# Integration tests (requires Docker)
docker compose up -d minio
./gradlew integTest

# Specific test class
./gradlew :server:test --tests "org.elasticsearch.index.IndexTests"
```

## Branch Strategy

```
main                 ← Stable releases only, merged via Release PR
  └── develop        ← Development mainline
       ├── feature/neural-hnsw      ← Phase 1 feature branches
       ├── feature/aero-s3          ← Phase 2 feature branches
       ├── feature/ranker-native    ← Phase 3 feature branches
       └── fix/lucene9-compat       ← Bug fix branches
```

- Create feature branches from `develop`
- Submit PRs to `develop`
- `main` is updated only via release PRs from `develop`

## Commit Convention

```
Format: <type>(<scope>): <subject>

Types:
  feat     — New feature
  fix      — Bug fix
  refactor — Code restructuring without behavior change
  perf     — Performance improvement
  test     — Adding or fixing tests
  docs     — Documentation only
  chore    — Build, tooling, dependency updates

Scopes:
  neural   — Neural Engine (vector search, HNSW, RRF)
  aero     — Aero Storage (S3, block cache, tiering)
  ranker   — Precision Ranker (native scoring, feature join)
  core     — Core engine (indexing, segments)
  api      — REST/Query API
  transport — Node communication
  common   — Shared utilities

Examples:
  feat(neural): integrate Lucene 9.x KnnVectorField
  fix(aero): resolve S3 segment read timeout on large blocks
  perf(ranker): optimize JNI bridge call overhead by 40%
```

## Code Style

- Follow existing Elasticsearch code conventions
- Java source files use 4-space indentation
- Maximum line length: 140 characters
- All public APIs must have Javadoc
- New code should target Java 17+ features where appropriate

## Pull Request Process

1. Ensure your branch is up to date with `develop`
2. All CI checks must pass
3. At least one reviewer approval is required
4. Squash merge is preferred for feature branches
5. PR description should include:
   - Summary of changes
   - Related issue/task number
   - Test plan
   - Performance impact (if applicable)

## Module Development Guide

### Adding to Neural Engine (`nexus-neural/`)
Vector indexing and hybrid search code. Dependencies: `server` (Lucene core).

### Adding to Aero Storage (`nexus-aero/`)
Storage abstraction and caching code. Dependencies: `server`.

### Adding to Precision Ranker (`nexus-ranker/`)
Scoring and ranking code. Native plugins require Rust toolchain.

## Reporting Issues

- Use GitHub Issues
- Include Nexus 710 version, JDK version, OS
- Provide minimal reproduction steps
- Attach relevant logs (`logs/` directory)

## License

By contributing, you agree that your contributions will be licensed under the Apache License 2.0.
