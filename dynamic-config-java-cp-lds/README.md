# Java envoy management server with LDS (Listener discovery service)

The listener discovery service (LDS) layers on a mechanism by which Envoy can discover entire listeners at runtime. This includes all filter stacks, up to and including HTTP filters with embedded references to RDS. Adding LDS into the mix allows almost every aspect of Envoy to be dynamically configured. Hot restart should only be required for very rare configuration changes (admin, tracing driver, etc.), certificate rotation, or binary updates.

## Building
```
cd dynamic-config-java-cp-lds/demo-service
mvn clean install -e
docker build -t demo-service:latest .

cd dynamic-config-java-cp-lds/envoy-java-management-server/
mvn clean install -e
docker build -t envoy-java-management-server:latest .

```

## Running
 * start java control plane
```
docker run --rm -p 12345:12345 envoy-java-management-server:latest
```
 *  start demo-service
 ```
docker run --rm -p 8000:8000 demo-service:latest
```
 * connect envoy using sample config file
```
cd dynamic-config-java-cp-lds
 envoy -c envoy/envoy.yaml
```
 * test api
```
curl -s http://localhost:10000
```
The management server could respond to LDS requests with:

```
version_info: "0"
resources:
- "@type": type.googleapis.com/envoy.config.listener.v3.Listener
  name: sample-listener
  address:
    socket_address:
      address: 127.0.0.1
      port_value: 10000
  filter_chains:
  - filters:
    - name: envoy.filters.network.http_connection_manager
      typed_config:
        "@type": type.googleapis.com/envoy.extensions.filters.network.http_connection_manager.v3.HttpConnectionManager
        stat_prefix: ingress_http
        codec_type: AUTO
        rds:
          route_config_name: local_route
          config_source:
            resource_api_version: V3
            api_config_source:
              api_type: GRPC
              transport_api_version: V3
              grpc_services:
                - envoy_grpc:
                    cluster_name: xds_cluster
        http_filters:
        - name: envoy.filters.http.router

```