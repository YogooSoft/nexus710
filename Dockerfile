FROM eclipse-temurin:17-jre-jammy AS base

ARG NEXUS_VERSION=7.10.2
ARG UID=1000
ARG GID=1000

RUN groupadd -g ${GID} nexus && \
    useradd -u ${UID} -g nexus -m -s /bin/bash nexus

RUN apt-get update && \
    apt-get install -y --no-install-recommends curl jq && \
    rm -rf /var/lib/apt/lists/*

ENV NEXUS_HOME=/usr/share/nexus710
ENV NEXUS_DATA=/var/lib/nexus710
ENV NEXUS_LOGS=/var/log/nexus710
ENV NEXUS_CONFIG=/etc/nexus710

RUN mkdir -p ${NEXUS_HOME} ${NEXUS_DATA} ${NEXUS_LOGS} ${NEXUS_CONFIG}

COPY distribution/archives/oss-linux-tar/build/distributions/nexus710-oss-*.tar.gz /tmp/nexus.tar.gz
RUN tar xzf /tmp/nexus.tar.gz --strip-components=1 -C ${NEXUS_HOME} && \
    rm -f /tmp/nexus.tar.gz

RUN chown -R nexus:nexus ${NEXUS_HOME} ${NEXUS_DATA} ${NEXUS_LOGS} ${NEXUS_CONFIG}

COPY <<'EOF' ${NEXUS_HOME}/config/nexus.yml
cluster.name: "nexus-cluster"
network.host: 0.0.0.0
discovery.type: single-node

nexus.neural.enabled: true
nexus.neural.offheap.enabled: true
nexus.neural.offheap.max_size: 4gb

nexus.aero.enabled: false
nexus.ranker.native.enabled: false
EOF

USER nexus

WORKDIR ${NEXUS_HOME}

EXPOSE 9200 9300

HEALTHCHECK --interval=30s --timeout=10s --retries=3 \
    CMD curl -sf http://localhost:9200/_cluster/health || exit 1

ENTRYPOINT ["bin/elasticsearch"]
