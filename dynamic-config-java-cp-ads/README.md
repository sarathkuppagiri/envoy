# Java envoy management server with ADS (Aggregated Discovery Service)
This example walks through configuring Envoy using the Java Control Plane reference implementation.

It demonstrates how configuration provided to Envoy persists, even when the control plane is not available, and provides a trivial example of how to update Envoy’s configuration dynamically.


## Building
```
cd dynamic-config-java-cp-ads/demo-service
mvn clean install -e
docker build -t demo-service:latest .

cd dynamic-config-java-cp-ads/envoy-java-management-server/
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
cd dynamic-config-java-cp-eds
 envoy -c envoy/envoy.yaml
```
 * test api
```
curl -s http://localhost:10000
```

The management server could respond to RDS requests with:

```
version_info: "0"
resources:
- "@type": type.googleapis.com/envoy.config.route.v3.RouteConfiguration
  name: local_route
  virtual_hosts:
  - name: local_service
    domains: ["*"]
    routes:
    - match: { prefix: "/" }
      route: { cluster: some_service }
```

The management server could respond to CDS requests with:

```
version_info: "0"
resources:
- "@type": type.googleapis.com/envoy.config.cluster.v3.Cluster
  name: some_service
  connect_timeout: 0.25s
  lb_policy: ROUND_ROBIN
  type: EDS
  eds_cluster_config:
    eds_config:
      resource_api_version: V3
      api_config_source:
        api_type: GRPC
        transport_api_version: V3
        grpc_services:
          - envoy_grpc:
              cluster_name: xds_cluster
```
The management server could respond to EDS requests with:

```
version_info: "0"
resources:
- "@type": type.googleapis.com/envoy.config.endpoint.v3.ClusterLoadAssignment
  cluster_name: some_service
  endpoints:
  - lb_endpoints:
    - endpoint:
        address:
          socket_address:
            address: 127.0.0.1
            port_value: 8000
```