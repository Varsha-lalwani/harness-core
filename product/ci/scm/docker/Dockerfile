# Create image for ci-scm service
#
# Build the ci-scm docker image using:
# > bazel build --platforms=@io_bazel_rules_go//go/toolchain:linux_amd64 //product/ci/scm/...
# > docker build -t harness/ci-scm:<tag> -f product/ci/scm/docker/Dockerfile $(bazel info bazel-bin)/product/ci/scm/

# First stage
FROM alpine:3.12
RUN GRPC_HEALTH_PROBE_VERSION=v0.4.11 && \
    wget -qO/bin/grpc_health_probe https://github.com/grpc-ecosystem/grpc-health-probe/releases/download/${GRPC_HEALTH_PROBE_VERSION}/grpc_health_probe-linux-amd64 && \
    chmod +x /bin/grpc_health_probe

# Second stage
FROM registry.access.redhat.com/ubi8/ubi-minimal:8.6-751
COPY --from=0 /bin/grpc_health_probe ./grpc_health_probe
# Copy ci-scm binary
COPY scm_/scm /usr/local/bin/scm
RUN chown -R 65534:65534 /usr/local/bin ./grpc_health_probe
USER 65534:65534
WORKDIR /usr/local/bin
CMD ["/usr/local/bin/scm"]