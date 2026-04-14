# Nexus 710 Baseline Performance Data

This directory stores baseline performance measurements for comparison across versions.

## Baseline Datasets

| Dataset | Suite | Scale | Purpose |
|---------|-------|-------|---------|
| ANN-Benchmarks SIFT1M | Vector Search | 1M x 128d | Vector recall & latency |
| ANN-Benchmarks GloVe-200 | Vector Search | 1.2M x 200d | High-dim vector perf |
| ES Rally geonames | Text Search | 11M docs | Text search regression |
| Custom hybrid dataset | Hybrid Search | 5M docs + vectors | Mixed query perf |
| TPC-H variant | Aggregation | 100GB | Join & aggregation perf |

## File Format

Baseline results are stored as JSON:

```json
{
  "suite": "TEXT_SEARCH_BASELINE",
  "version": "7.10.2",
  "timestamp": "2026-04-14T00:00:00Z",
  "environment": {
    "cpu": "...",
    "memory": "...",
    "disk": "...",
    "jdk": "17.0.9"
  },
  "metrics": {
    "indexing_throughput_docs_per_sec": 0,
    "query_p50_ms": 0,
    "query_p99_ms": 0,
    "query_p999_ms": 0,
    "qps": 0,
    "recall_at_10": 0
  }
}
```

## Running Baselines

```bash
# Generate ES 7.10.2 baseline (requires running cluster)
./gradlew :nexus-benchmark:run --args="--suite TEXT_SEARCH_BASELINE --output baselines/"
```
