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
