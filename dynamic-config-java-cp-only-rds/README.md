# Java envoy management server with CDS (Cluster discovery service) and RDS (Route discovery service)
This example walks through configuring Envoy using the Java Control Plane reference implementation.

The route discovery service (RDS) API layers on a mechanism by which Envoy can discover the entire route configuration for an HTTP connection manager filter at runtime. The route configuration will be gracefully swapped in without affecting existing requests. This API, when used alongside EDS and CDS, allows implementors to build a complex routing topology (traffic shifting, blue/green deployment, etc).



## Building
```
cd dynamic-config-java-cp-only-rds/demo-service
mvn clean install -e
docker build -t demo-service:latest .

cd dynamic-config-java-cp-only-rds/envoy-java-management-server/
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
cd dynamic-config-java-cp-only-rds
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